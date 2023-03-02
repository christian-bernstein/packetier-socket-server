package de.christianbernstein.packetier.engine

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Packet(
    val type: String,
    var packetType: PacketType,
    var conversationID: String,
    val data: Map<String, String>,
) {
    constructor(type: String, packetType: PacketType = PacketType.SINGLETON) :
            this(type, packetType, UUID.randomUUID().toString(), mapOf())

    constructor(type: String, data: Map<String, Any>) :
            this(type, PacketType.SINGLETON, UUID.randomUUID().toString(), data.map { it.key to it.value.toString() }.associate { it })

    fun getInt(key: String): Int = this.data.getValue(key).toInt()

    fun getString(key: String): String = this.data.getValue(key)

}
