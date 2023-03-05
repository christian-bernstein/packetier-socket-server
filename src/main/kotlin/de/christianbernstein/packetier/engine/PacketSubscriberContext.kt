package de.christianbernstein.packetier.engine

data class PacketSubscriberContext(
    val engine: PacketEngine,
    val packet: Packet,
    val senderID: String,
    val receiverID: String,

    val session: Session,
    // TODO: Add connection param
    // val connection: Connection
) {
    fun send(packet: Packet): Unit = this.engine.pub(senderID = this.receiverID, receiverID = this.senderID, packet)

    fun send(type: String, data: Map<String, String>): Unit = this.engine.pub(senderID = this.receiverID, receiverID = this.senderID, packet = Packet(
        type = type,
        packetType = PacketType.SINGLETON,
        conversationID = this.packet.conversationID,
        data = data
    )
    )

    fun respond(data: Map<String, Any>): Unit = this.engine.pub(senderID = this.receiverID, receiverID = this.senderID, packet = Packet(
        type = "response@${this.packet.type}",
        packetType = PacketType.RESPONSE,
        conversationID = this.packet.conversationID,
        data = data.map { it.key to it.value.toString() }.associate { it }
    ))

    fun generateEndpointResponse(
        success: Boolean = true,
        code: Int = EndpointErrorCodes.SUCCESS.code,
        additionalMessage: String? = null,
        data: Map<String, Any> = mapOf()
    ): Map<String, String> = mutableMapOf(
        "success" to success.toString(),
        "code" to code.toString(),
        "has-additional-message" to (additionalMessage != null).toString(),
        "additional-message" to (additionalMessage ?: "No additional message provided")
    ).also { map ->
        data.forEach { dataEntry ->
            map[dataEntry.key] = dataEntry.value.toString()
        }
    }

    fun finishWithEmptySuccess() = this.respond(this.generateEndpointResponse())

    fun finishWithInternalError(message: String? = null) = this.respond(this.generateEndpointResponse(
        success = false,
        code = EndpointErrorCodes.INTERNAL_SERVER_ERROR.code,
        additionalMessage = message
    ))

    fun finishWithAuthorizationError(message: String? = null) = this.respond(this.generateEndpointResponse(
        success = false,
        code = EndpointErrorCodes.UNAUTHORIZED.code,
        additionalMessage = message
    ))
}
