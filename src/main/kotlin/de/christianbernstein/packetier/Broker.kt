package de.christianbernstein.packetier

import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.PacketEngine
import de.christianbernstein.packetier.engine.PacketierNetAdapter
import de.christianbernstein.packetier.engine.packets.ActivationPacket
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
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.collections.LinkedHashSet
import kotlin.concurrent.thread

@Suppress("ExtractKtorModule")
class Broker {

    companion object {
        const val PACKETIER_SERVER_ID = "packetier-server"
        var INSTANCE: Broker? = null
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

    private lateinit var socketEngine: NettyApplicationEngine

    var packetEngine: PacketEngine = PacketEngine(this.generatePacketierBridge())

    init {
        if (INSTANCE != null) throw IllegalStateException("Broker can only be initialized once")
        INSTANCE = this
    }

    fun shutdown() {
        this.socketEngine.stop(10, 10, TimeUnit.SECONDS)
    }

    private fun generatePacketierBridge(): PacketierNetAdapter = object : PacketierNetAdapter() {

        override fun pub(senderID: String, receiverID: String, packet: Packet): Unit = this@Broker
            .connections
            .first { it.id.toString() == receiverID }
            .socketSession
            .run {
                launch {
                    this@run.send(Json.encodeToString(packet))
                }
            }

        override fun broadPub(senderID: String, packet: Packet): Unit = Json.encodeToString(packet).let { msg ->
            this@Broker.connections.forEach {
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

    private fun startEmbeddedServer(wait: Boolean): NettyApplicationEngine = embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        this@Broker.initMainSocketRoute(this)
    }.also { this@Broker.socketEngine = it }.start(wait = wait)

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
                    this@Broker.onConnectionInit(con)
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        this@Broker.onMessage(con, frame.readText())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    this@Broker.onConnectionClose(con)
                }
            }
        }
    }

    private fun onMessage(connection: Connection, data: String) {
        this.packetEngine.handle(
            senderID = connection.id,
            receiverID = PACKETIER_SERVER_ID,
            packet = Json.decodeFromString(Packet.serializer(), data)
        )
    }

    private fun onConnectionClose(connection: Connection) {
        logger.debug("Closing connection ${connection.id}")
        this.packetEngine.closeSession(connection.id)
        connections -= connection
    }

    private suspend fun onConnectionInit(connection: Connection) {
        logger.debug("Initiate connection ${connection.id}")
        this.packetEngine.createSession(connection.id)
        connections += connection
        logger.debug("Sending activation packet to connection ${connection.id}")
        this.sendActivationPacket(connection.id)
    }

    private fun getConnection(connectionID: String): Connection = this.connections.first { it.id == connectionID }

    private suspend fun sendActivationPacket(connectionID: String) = this.sendPacket(connectionID, ActivationPacket(connectionID))

    private suspend fun sendPacket(connectionID: String, packet: Packet) = this.getConnection(connectionID).socketSession.send(Json.encodeToString(packet))

    /**
     * TODO: Store salt in file / db
     */
    fun generateToken(secret: String): String = SecretKeyFactory
        .getInstance("PBKDF2WithHmacSHA1")
        .generateSecret(PBEKeySpec(secret.toCharArray(), "salty salted salt".toByteArray(), 65536, 24))
        .encoded
        .let { Base64.getUrlEncoder().encodeToString(it) }
}

fun broker(): Broker = Broker.INSTANCE ?: Broker()
