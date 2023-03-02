package de.christianbernstein.packetier.engine

import de.christianbernstein.packetier.engine.PacketSubscriber
import de.christianbernstein.packetier.engine.PacketSubscriberContext
import java.lang.Exception

fun createProtocol(vararg channels: Pair<String, PacketSubscriberContext.() -> Unit>): PacketSubscriber = with(mapOf(*channels)) {{
    val type = it.packet.type
    val handler = get(type) ?: throw Exception("Protocol doesn't handle packets of type '$type'")
    handler(it)
}}
