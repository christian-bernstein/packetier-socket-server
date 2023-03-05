package de.christianbernstein.packetier

import io.ktor.websocket.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

data class Connection(
    val session: DefaultWebSocketSession,
    val id: String = UUID.randomUUID().toString()
)
