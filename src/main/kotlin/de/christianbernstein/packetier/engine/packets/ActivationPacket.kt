package de.christianbernstein.packetier.engine.packets

import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.PacketLayerType

class ActivationPacket(
    internalSocketConnectionID: String
): Packet(
    type = "ActivationPacket",
    layer = PacketLayerType.ENGINE,
    data = mapOf(
        "internalSocketConnectionID" to internalSocketConnectionID
    )
)
