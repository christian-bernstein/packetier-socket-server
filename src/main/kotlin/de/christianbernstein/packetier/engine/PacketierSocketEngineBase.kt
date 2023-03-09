package de.christianbernstein.packetier.engine

import de.christianbernstein.packetier.Broker

abstract class PacketierSocketEngineBase {
    lateinit var broker: Broker
    abstract fun start(wait: Boolean = false)
}
