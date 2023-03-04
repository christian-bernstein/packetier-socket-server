package de.christianbernstein.packetier.engine.events

import de.christianbernstein.packetier.engine.PacketSubscriberContext
import de.christianbernstein.packetier.event.Event

data class SessionPacketReceivedEvent(
    val ctx: PacketSubscriberContext
): Event("SessionMessageReceivedEvent")
