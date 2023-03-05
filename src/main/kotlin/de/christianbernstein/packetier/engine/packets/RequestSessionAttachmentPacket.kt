package de.christianbernstein.packetier.engine.packets

import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.PacketLayerType

class RequestSessionAttachmentPacket(sessionID: String): Packet(
    type = "RequestSessionAttachmentPacket",
    layer = PacketLayerType.ENGINE,
    data = mapOf("sessionID" to sessionID)
)
