package de.christianbernstein.packetier

import de.christianbernstein.packetier.endpoints.RequestSessionAttachmentPacketEndpoint
import de.christianbernstein.packetier.events.SessionPacketReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class PacketEngine(private val netAdapter: PacketierNetAdapter) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private val sessions: MutableMap<String, Session> = mutableMapOf()

    private val responseContracts: MutableMap<String, ResponseContract> = mutableMapOf()

    private val enginePacketLayer: PacketSubscriber = createProtocol(
        RequestSessionAttachmentPacketEndpoint().pair()
    )

    init {
        this.netAdapter.init(this)
    }

    fun pub(senderID: String, receiverID: String, packet: Packet): Unit = this.netAdapter.pub(senderID, receiverID, packet)

    fun broadPub(senderID: String, packet: Packet): Unit = this.netAdapter.broadPub(senderID, packet)

    private fun getAllSessions(): Set<Session> = this.sessions.values.toSet()

    fun getSession(id: String): Session = this.getAllSessions().first { it.id == id }

    fun createSession(id: String = UUID.randomUUID().toString(), subscriber: PacketSubscriber = createProtocol(), init: Session.() -> Unit = {}): Session {
        val tokenBase: String = UUID.randomUUID().toString()
        val token = broker().generateToken(tokenBase)
        return Session(id, this, subscriber, publicToken = tokenBase, privateToken = token)
            .also { this.sessions[id] = it }
            .apply(init)
    }

    fun closeSession(id: String) {
        this.sessions.remove(id).run {
            if (this == null) return
            onSessionClosed()
        }
    }

    fun fetch(senderID: String, receiverID: String, packet: Packet, onResolve: Packet.() -> Unit) {
        val convID = UUID.randomUUID().toString()
        packet.packetType = PacketType.REQUEST
        packet.conversationID = convID
        this.responseContracts[convID] = ResponseContract(convID, onResolve = { onResolve(it) })
        this.pub(senderID, receiverID, packet)
    }

    private fun handleEngineLayerPacket(senderID: String, receiverID: String, packet: Packet) {
        val engineSubscriberContext = PacketSubscriberContext(senderID = senderID, receiverID = receiverID, engine = this@PacketEngine, packet = packet, session = null)
        this@PacketEngine.enginePacketLayer(engineSubscriberContext)
    }

    private fun handleResponsePacket(senderID: String, receiverID: String, packet: Packet) {
        this@PacketEngine.responseContracts.remove(packet.conversationID).run {
            if (this == null) return@run
            onResolve.accept(packet)
        }
    }

    fun handle(senderID: String, receiverID: String, packet: Packet) {
        if (packet.packetType == PacketType.RESPONSE) {
            this.handleResponsePacket(senderID, receiverID, packet)
            return
        }

        if (packet.layer == PacketLayerType.ENGINE) {
            this.handleEngineLayerPacket(senderID, receiverID, packet)
            return
        }

        try {
            with(requireNotNull(sessions[receiverID]) { "Session '$receiverID' wasn't found." }) {
                val sessionSubscriberContext = PacketSubscriberContext(senderID = senderID, receiverID = receiverID, engine = this@PacketEngine, packet = packet, session = this)
                this.bus.fire(SessionPacketReceivedEvent(sessionSubscriberContext))
                this.subscriber(sessionSubscriberContext)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
