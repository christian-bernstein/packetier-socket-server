package de.christianbernstein.packetier.engine.endpoints

import de.christianbernstein.packetier.broker
import de.christianbernstein.packetier.engine.Endpoint

class RequestSessionAttachmentPacketEndpoint: Endpoint("RequestSessionAttachmentPacket", {
    val sessionID = packet.getString("sessionID")
    val sessionPublicToken = packet.getString("token")
    val sessionPrivateToken = broker().generateToken(sessionPublicToken)
    val targetSession = broker().packetEngine.getSession(sessionID)

    if (targetSession.privateToken != sessionPrivateToken) {
        // Authentication failed, session won't be attached
        finishWithAuthorizationError("Provided token was incorrect for selected session")
    } else {
        // TODO: Check if senderID is correct
        // Authentication succeeded, connection will be attached to selected session
        broker().getConnection(senderID).packetEngineSessionId = sessionID
        finishWithEmptySuccess()
    }
})
