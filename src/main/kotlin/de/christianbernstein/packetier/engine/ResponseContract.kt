package de.christianbernstein.packetier.engine

import de.christianbernstein.packetier.engine.Packet
import java.util.function.Consumer

data class ResponseContract(
    val conversationID: String,
    val onResolve: Consumer<Packet>
)
