package de.christianbernstein.packetier.debug.packets

import de.christianbernstein.packetier.engine.Packet

class TestPacket(message: String = "No message provided"): Packet(
    type = "TestPacket",
    data = mapOf(
        "message" to message
    )
)
