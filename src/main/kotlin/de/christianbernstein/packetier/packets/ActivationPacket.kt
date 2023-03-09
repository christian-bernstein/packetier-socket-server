package de.christianbernstein.packetier.packets

import de.christianbernstein.packetier.Packet
import de.christianbernstein.packetier.PacketLayerType

class ActivationPacket(
    internalSocketConnectionID: String
): Packet(
    type = "ActivationPacket",
    layer = PacketLayerType.ENGINE,
    data = mapOf(
        "internalSocketConnectionID" to internalSocketConnectionID
    )
)
