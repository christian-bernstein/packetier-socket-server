package de.christianbernstein.packetier.debug.packets

import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.PacketLayerType

class RequestSessionAttachmentPacket(sessionID: String, token: String): Packet(
    type = "RequestSessionAttachmentPacket",
    layer = PacketLayerType.ENGINE,
    data = mapOf(
        "sessionID" to sessionID,
        "token" to token
    )
)
