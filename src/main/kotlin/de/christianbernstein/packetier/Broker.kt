package de.christianbernstein.packetier

import de.christianbernstein.packetier.engine.PacketierSocketEngineBase
import de.christianbernstein.packetier.packets.ActivationPacket
import de.christianbernstein.packetier.engine.ktor.KtorConnection
import de.christianbernstein.packetier.engine.ktor.KtorEngine
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

@Suppress("ExtractKtorModule")
class Broker<T : PacketierSocketEngineBase<*>>(val socketEngine: T) {

    companion object {
        const val PACKETIER_SERVER_ID = "packetier-server"
        var INSTANCE: Broker<*>? = null
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)

    var packetEngine: PacketEngine = PacketEngine(this.socketEngine.generatePacketierBridge())

    init {
        if (INSTANCE != null) throw IllegalStateException("Broker can only be initialized once")
        INSTANCE = this
    }

    fun shutdown() {
        this.socketEngine.shutdown()
    }

    fun init(wait: Boolean = false) {
        this.socketEngine.start(wait)
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
