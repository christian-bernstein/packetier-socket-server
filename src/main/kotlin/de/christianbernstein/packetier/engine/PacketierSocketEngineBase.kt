package de.christianbernstein.packetier.engine

import de.christianbernstein.packetier.Broker
import de.christianbernstein.packetier.PacketierNetAdapter
import java.util.concurrent.TimeUnit

abstract class PacketierSocketEngineBase<T : EngineSession> {
    lateinit var broker: Broker<*>
    abstract fun start(wait: Boolean = false)
    abstract fun shutdown()
    abstract fun generatePacketierBridge(): PacketierNetAdapter

    abstract fun getEngineSession(id: String): T
}
