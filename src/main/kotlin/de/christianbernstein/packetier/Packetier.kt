package de.christianbernstein.packetier

import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.PacketEngine
import de.christianbernstein.packetier.engine.PacketierNetAdapter
import de.christianbernstein.packetier.server.Connection
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.LinkedHashSet
import kotlin.concurrent.thread

@Suppress("ExtractKtorModule")
class Packetier {

    init {

    }

    companion object {
        const val PACKETIER_SERVER_ID = "packetier-server"
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

    private lateinit var socketEngine: NettyApplicationEngine

    var packetEngine: PacketEngine = PacketEngine(this.generatePacketierBridge())

    fun shutdown() {
        this.socketEngine.stop(10, 10, TimeUnit.SECONDS)
    }

    private fun generatePacketierBridge(): PacketierNetAdapter = object : PacketierNetAdapter() {

        override fun pub(senderID: String, receiverID: String, packet: Packet): Unit = this@Packetier
            .connections
            .first { it.id.toString() == receiverID }
            .session
            .run {
                launch {
                    this@run.send(Json.encodeToString(packet))
                }
            }

        override fun broadPub(senderID: String, packet: Packet): Unit = Json.encodeToString(packet).let { msg ->
            this@Packetier.connections.forEach {
                it.session.run {
                    launch {
                        this@run.send(msg)
                    }
                }
            }
        }
    }

    private fun startEmbeddedServer(wait: Boolean): NettyApplicationEngine = embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        this@Packetier.initMainSocketRoute(this)
    }.also { this@Packetier.socketEngine = it }.start(wait = true)


    /**
     * @param wait
     *  IF true: After ktor init: Wait here.
     *  IF false: After ktor init: Complete future & return from init method -> If no thread blocking JVM: JVM stopps
     */
    fun init(wait: Boolean = false) {
        if (wait) {
            // After ktor init: Wait here
            this.startEmbeddedServer(true)
        } else {
            // After ktor init: Complete future & return from init method -> If no thread blocking JVM: JVM stopps
            CompletableDeferred<Unit>().run {
                thread(start = true) {
                    startEmbeddedServer(false).also {
                        this.complete(Unit)
                    }
                }
                runBlocking { this@run.await() }
            }
        }
    }

    private fun initMainSocketRoute(application: Application): Unit {
        application.routing {
            webSocket("main") {
                val con = Connection(this)
                try {
                    this@Packetier.onConnectionInit(con)
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        this@Packetier.onMessage(con, frame.readText())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    this@Packetier.onConnectionClose(con)
                }
            }
        }
    }

    private fun onMessage(connection: Connection, data: String) {
        this.packetEngine.handle(
            senderID = connection.id.toString(),
            receiverID = PACKETIER_SERVER_ID,
            packet = Json.decodeFromString(Packet.serializer(), data)
        )
    }

    private fun onConnectionClose(connection: Connection) {
        this.packetEngine.closeSession(connection.id.toString())
        connections -= connection
    }

    private fun onConnectionInit(connection: Connection) {
        this.packetEngine.createSession(connection.id.toString())
        connections += connection
    }
}
