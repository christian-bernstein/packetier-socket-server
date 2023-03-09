package de.christianbernstein.packetier

import de.christianbernstein.packetier.engines.PacketierSocketEngineBase
import de.christianbernstein.packetier.engines.ktor.KtorEngine
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
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Suppress("ExtractKtorModule", "MemberVisibilityCanBePrivate")
class Broker<T : PacketierSocketEngineBase<*>>(val socketEngine: T) {

    companion object {
        const val PACKETIER_SERVER_ID = "packetier-server"
        var INSTANCE: Broker<*>? = null
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val brokerSessions = mutableListOf<BrokerSession>()

    private var startedFlag: Boolean = false

    var packetEngine: PacketEngine = PacketEngine(this.socketEngine.generatePacketierBridge())

    init {
        if (INSTANCE != null) throw IllegalStateException("Broker can only be initialized once")
        INSTANCE = this
        this.socketEngine.broker = this
    }

    fun shutdown() {
        this.socketEngine.shutdown()
    }

    fun init(wait: Boolean = false) {
        if (startedFlag) throw IllegalStateException("Cannot init broker, broker is already marked as started")
        this.startedFlag = true
        this.socketEngine.start(wait)
    }

    fun createBrokerSession(id: String, createPacketSession: Boolean = false): BrokerSession {
        if (createPacketSession) this.packetEngine.createSession(id)
        return BrokerSession(
            internalID = id,
            engineSessionID = id,
            packetierSessionID = if (createPacketSession) id else null
        ).also { this.brokerSessions += it }
    }

    fun getBrokerSession(id: String): BrokerSession = this.brokerSessions.first { it.internalID == id }

    fun updateBrokerSessionToPacketierSessionLinkage(brokerSessionID: String, newPacketierSessionID: String) {
        this.getBrokerSession(brokerSessionID).packetierSessionID = newPacketierSessionID
    }

    /**
     * TODO: Store salt in file / db
     */
    fun generateToken(secret: String): String = SecretKeyFactory
        .getInstance("PBKDF2WithHmacSHA1")
        .generateSecret(PBEKeySpec(secret.toCharArray(), "salty salted salt".toByteArray(), 65536, 24))
        .encoded
        .let { Base64.getUrlEncoder().encodeToString(it) }
}

fun broker(): Broker<*> = Broker.INSTANCE ?: Broker(KtorEngine)

@Suppress("UNCHECKED_CAST")
fun <T : PacketierSocketEngineBase<*>> broker(engine: T): Broker<T> {
    return if (Broker.INSTANCE == null) { Broker(engine) } else { Broker.INSTANCE as Broker<T> }
}
