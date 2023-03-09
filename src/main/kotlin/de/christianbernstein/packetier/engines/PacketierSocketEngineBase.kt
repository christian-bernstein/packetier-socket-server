package de.christianbernstein.packetier.engines

import de.christianbernstein.packetier.Broker
import de.christianbernstein.packetier.PacketierNetAdapter

abstract class PacketierSocketEngineBase<T : EngineSession> {
    lateinit var broker: Broker<*>
    abstract fun start(wait: Boolean = false)
    abstract fun shutdown()
    abstract fun generatePacketierBridge(): PacketierNetAdapter

    abstract fun getEngineSession(id: String): T
}
