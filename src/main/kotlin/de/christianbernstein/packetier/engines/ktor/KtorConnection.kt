package de.christianbernstein.packetier.engines.ktor

import de.christianbernstein.packetier.engines.EngineSession
import io.ktor.websocket.*
import java.util.UUID

data class KtorConnection(
    val socketSession: DefaultWebSocketSession,
    val ktorConnectionId: String = UUID.randomUUID().toString(),
    var packetEngineSessionId: String = ktorConnectionId,
) : EngineSession(
    id = ktorConnectionId
)
