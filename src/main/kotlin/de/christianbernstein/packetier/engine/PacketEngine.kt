package de.christianbernstein.packetier.engine

import de.christianbernstein.packetier.engine.events.SessionPacketReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class PacketEngine(private val netAdapter: PacketierNetAdapter) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private val sessions: MutableMap<String, Session> = mutableMapOf()

    private val responseContracts: MutableMap<String, ResponseContract> = mutableMapOf()

    private val enginePacketLayer: PacketSubscriber = createProtocol(
        "" to {

        }
    )

    init {
        this.netAdapter.init(this)
    }

    fun pub(senderID: String, receiverID: String, packet: Packet): Unit = this.netAdapter.pub(senderID, receiverID, packet)

    fun broadPub(senderID: String, packet: Packet): Unit = this.netAdapter.broadPub(senderID, packet)

    private fun getAllSessions(): Set<Session> = this.sessions.values.toSet()

    fun getSession(id: String): Session = this.getAllSessions().first { it.id == id }

    fun createSession(id: String = UUID.randomUUID().toString(), subscriber: PacketSubscriber = createProtocol(), init: Session.() -> Unit = {}): Session {
        return Session(id, this, subscriber)
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

    fun handle(senderID: String, receiverID: String, packet: Packet): Unit = with(requireNotNull(sessions[receiverID]) { "Session '$receiverID' wasn't found." }) {
        try {
            val subscriberContext = PacketSubscriberContext(senderID = senderID, receiverID = receiverID, engine = this@PacketEngine, packet = packet, session = this)

            // TODO: Remove
            // Fire pre-handling, generic message event
            println("About to fire SessionPacketReceivedEvent")


            this.bus.fire(SessionPacketReceivedEvent(subscriberContext))
            if (packet.packetType == PacketType.RESPONSE) {
                this@PacketEngine.responseContracts.remove(packet.conversationID).run {
                    if (this == null) return@run
                    onResolve.accept(packet)
                }
                return@with
            }

            // Todo handle engine layer
            // Handle engine packet-layer messages
            if (packet.layer == PacketLayerType.ENGINE) {

                return@with
            }

            this.subscriber(subscriberContext)
        } catch (e: Exception) {
            this@PacketEngine.logger.error("Error while handling packet")
            e.printStackTrace()
        }
    }
}
