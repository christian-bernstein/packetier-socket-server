package de.christianbernstein.packetier

import java.util.function.Consumer

data class ResponseContract(
    val conversationID: String,
    val onResolve: Consumer<Packet>
)
