package de.christianbernstein.packetier

data class BrokerSession(
    val internalID: String,
    val engineSessionID: String,
    var packetierSessionID: String?,
)
