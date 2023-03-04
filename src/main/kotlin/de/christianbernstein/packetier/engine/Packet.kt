package de.christianbernstein.packetier.engine

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
open class Packet(
    val type: String,
    var packetType: PacketType = PacketType.SINGLETON,
    var conversationID: String = UUID.randomUUID().toString(),
    var data: Map<String, String> = emptyMap(),
    var layer: PacketLayerType = PacketLayerType.USER
) {
    // TODO: Remove
    constructor(type: String, packetType: PacketType = PacketType.SINGLETON) :
            this(type, packetType, UUID.randomUUID().toString(), mapOf())

    // TODO: Remove
    constructor(type: String, data: Map<String, Any>) :
            this(type, PacketType.SINGLETON, UUID.randomUUID().toString(), data.map { it.key to it.value.toString() }.associate { it })

    fun getInt(key: String): Int = this.data.getValue(key).toInt()

    fun getString(key: String): String = this.data.getValue(key)

}
