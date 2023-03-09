package de.christianbernstein.packetier.socketEngines.ktor

import io.ktor.websocket.*
import java.util.UUID

data class KtorConnection(
    val socketSession: DefaultWebSocketSession,
    val id: String = UUID.randomUUID().toString(),
    var packetEngineSessionId: String = id
)
