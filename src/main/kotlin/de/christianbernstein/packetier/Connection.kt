package de.christianbernstein.packetier

import io.ktor.websocket.*
import java.util.UUID

data class Connection(
    val socketSession: DefaultWebSocketSession,
    val id: String = UUID.randomUUID().toString(),
    var packetEngineSessionId: String = id
)
