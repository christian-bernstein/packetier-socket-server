package de.christianbernstein.packetier.debug

import de.christianbernstein.packetier.engine.Packet
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PacketierDebuggingClient(
    var name: String = UUID.randomUUID().toString(),
    var path: String = "/main",
    val configurator: PacketierDebuggingClient.() -> Unit = {},
    val suite: suspend PacketierDebuggingClient.() -> Unit
) {

    lateinit var http: HttpClient

    lateinit var socket: DefaultClientWebSocketSession

    var thread: Thread? = null

    var async: Boolean = true

    var skipTest: Boolean = false

    var globalPacketInterceptors: MutableMap<String, (packet: Packet) -> Unit> = mutableMapOf()

    init {
        this.init()
    }

    private fun init() {
        this.configurator()
        if (this.skipTest) return
        log("Starting client '${this.name}' (async: ${if (this.async) "enabled" else "disabled"})")
        if (this.async) {
            this.thread = thread(start = true) { this.start() }
        } else {
            this.start()
        }
    }

    private fun start() = runBlocking {
        HttpClient { install(WebSockets) }.also { this@PacketierDebuggingClient.http = it }.run {
            webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = this@PacketierDebuggingClient.path) {
                this@PacketierDebuggingClient.socket = this
                val readMessageRoutine = launch { readMessages() }
                val userSuitRoutine = launch { userSuit() }
                readMessageRoutine.join()
                userSuitRoutine.cancelAndJoin()
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.readMessages() {
        try {
            for (message in incoming) {
                message as? Frame.Text ?: continue
                val rawMessage = message.readText()
                val packet: Packet = Json.decodeFromString(rawMessage)
                log("Packet received: '${rawMessage}'")
                try {
                    this@PacketierDebuggingClient.globalPacketInterceptors.values.forEach { it(packet) }
                } catch (e: Exception) {
                    error("Exception while handling packet")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            log("Error while receiving: ${e.localizedMessage}")
        } finally {
            log("Closing iris server socket connection")
        }
    }

    private suspend fun userSuit() = this.suite()

    suspend fun send(packet: Packet) = this.socket.send(Json.encodeToString(value = packet))

    suspend fun test(skip: Boolean = false, test: suspend () -> Unit) {
        try {
            if (!skip) test()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addPacketInterceptor(interceptorID: String = UUID.randomUUID().toString(), interceptor: (packet: Packet) -> Unit): String {
        this.globalPacketInterceptors[interceptorID] = interceptor
        return interceptorID
    }

    fun log(message: Any) = LoggerFactory
        .getLogger(this.javaClass)
        .debug("[${this.name}] $message")

    fun error(message: Any) = LoggerFactory
        .getLogger(this.javaClass)
        .error("[${this.name}] $message")

    fun awaitPacket(predicate: (packet: Packet) -> Boolean): Packet = CompletableDeferred<Packet>().run {
        runBlocking {
            val interceptorID = UUID.randomUUID().toString()
            addPacketInterceptor(interceptorID) {
                if (predicate(it)) {
                    this@PacketierDebuggingClient.globalPacketInterceptors.remove(interceptorID)
                    this@run.complete(it)
                }
            }
        }
        runBlocking { this@run.await() }
    }
}
