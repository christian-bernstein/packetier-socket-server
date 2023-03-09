package de.christianbernstein.packetier.debug.packets

import de.christianbernstein.packetier.Packet

class TestPacket(message: String = "No message provided"): Packet(
    type = "TestPacket",
    data = mapOf(
        "message" to message
    )
)
