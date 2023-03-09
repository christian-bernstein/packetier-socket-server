package de.christianbernstein.packetier.events

import de.christianbernstein.packetier.PacketSubscriberContext
import de.christianbernstein.packetier.Event

data class SessionPacketReceivedEvent(
    val ctx: PacketSubscriberContext
): Event("SessionPacketReceivedEvent")
