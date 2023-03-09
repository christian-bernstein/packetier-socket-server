package de.christianbernstein.packetier.engines.ktor

import de.christianbernstein.packetier.Broker
import de.christianbernstein.packetier.Packet
import de.christianbernstein.packetier.PacketierNetAdapter
import de.christianbernstein.packetier.engines.PacketierSocketEngineBase
import de.christianbernstein.packetier.packets.ActivationPacket
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.LinkedHashSet
import kotlin.concurrent.thread

object KtorEngine : PacketierSocketEngineBase<KtorConnection>() {

    private val connections = Collections.synchronizedSet<KtorConnection?>(LinkedHashSet())

    private lateinit var socketEngine: NettyApplicationEngine

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * @param wait
     *  IF true: After ktor init: Wait here.
     *  IF false: After ktor init: Complete future & return from init method -> If no thread blocking JVM: JVM stopps
     */
    override fun start(wait: Boolean) {
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

    override fun shutdown() {
        this.socketEngine.stop(10, 10, TimeUnit.SECONDS)
    }

    override fun generatePacketierBridge(): PacketierNetAdapter = object : PacketierNetAdapter() {

        override fun pub(senderID: String, receiverID: String, packet: Packet): Unit = this@KtorEngine
            .connections
            .first { it.id.toString() == receiverID }
            .socketSession
            .run {
                launch {
                    this@run.send(Json.encodeToString(packet))
                }
            }

        override fun broadPub(senderID: String, packet: Packet): Unit = Json.encodeToString(packet).let { msg ->
            this@KtorEngine.connections.forEach {
                it.socketSession.run {
                    launch {
                        try {
                            this@run.send(msg)
                        } catch (e: Exception) {
                            logger.warn("${e.javaClass.name} while broadcasting message: ${e.localizedMessage}")
                        }
                    }
                }
            }
        }
    }

    @Suppress("ExtractKtorModule")
    private fun startEmbeddedServer(wait: Boolean): NettyApplicationEngine = embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        this@KtorEngine.initMainSocketRoute(this)
    }.also { this@KtorEngine.socketEngine = it }.start(wait = wait)

    private fun initMainSocketRoute(application: Application): Unit {
        application.routing {
            webSocket("main") {
                val con = KtorConnection(this)
                try {
                    this@KtorEngine.onConnectionInit(con)
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        this@KtorEngine.onMessage(con, frame.readText())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    this@KtorEngine.onConnectionClose(con)
                }
            }
        }
    }

    private fun onMessage(connection: KtorConnection, data: String) {
        this.broker.packetEngine.handle(
            senderID = connection.id,
            receiverID = Broker.PACKETIER_SERVER_ID,
            packet = Json.decodeFromString(Packet.serializer(), data)
        )
    }

    private fun onConnectionClose(connection: KtorConnection) {
        logger.debug("Closing connection ${connection.id}")
        this.broker.packetEngine.closeSession(connection.id)
        connections -= connection
    }

    private suspend fun onConnectionInit(connection: KtorConnection) {
        logger.debug("Initiate connection ${connection.id}")
        this.broker.packetEngine.createSession(connection.id)
        connections += connection
        logger.debug("Sending activation packet to connection ${connection.id}")
        this.sendActivationPacket(connection.id)
    }

    override fun getEngineSession(id: String): KtorConnection = this.connections.first { it.id == id }

    private suspend fun sendActivationPacket(connectionID: String) = this.sendPacket(connectionID, ActivationPacket(connectionID))

    private suspend fun sendPacket(connectionID: String, packet: Packet) = this.getEngineSession(connectionID).socketSession.send(
        Json.encodeToString(packet))

}
