package de.christianbernstein.packetier.engine

abstract class PacketierNetAdapter {

    lateinit var engine: PacketEngine

    abstract fun pub(senderID: String, receiverID: String, packet: Packet)

    abstract fun broadPub(senderID: String, packet: Packet)

    fun init(engine: PacketEngine) { this.engine = engine }

}
