package de.christianbernstein.packetier.debug.packets

import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.PacketLayerType
import de.christianbernstein.packetier.engine.PacketType

class RequestSessionAttachmentPacket(sessionID: String, token: String): Packet(
    type = "RequestSessionAttachmentPacket",
    layer = PacketLayerType.ENGINE,
    packetType = PacketType.REQUEST,
    data = mapOf(
        "sessionID" to sessionID,
        "token" to token
    )
)
