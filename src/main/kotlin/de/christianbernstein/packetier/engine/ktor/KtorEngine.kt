package de.christianbernstein.packetier.engine.ktor

import de.christianbernstein.packetier.Broker
import de.christianbernstein.packetier.Packet
import de.christianbernstein.packetier.engine.PacketierSocketEngineBase
import de.christianbernstein.packetier.packets.ActivationPacket
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.LinkedHashSet
import kotlin.concurrent.thread

class KtorEngine: PacketierSocketEngineBase() {

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

    private fun getConnection(connectionID: String): KtorConnection = this.connections.first { it.id == connectionID }

    private suspend fun sendActivationPacket(connectionID: String) = this.sendPacket(connectionID, ActivationPacket(connectionID))

    private suspend fun sendPacket(connectionID: String, packet: Packet) = this.getConnection(connectionID).socketSession.send(
        Json.encodeToString(packet))

}
