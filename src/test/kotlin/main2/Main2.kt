package main2

import de.christianbernstein.packetier.Broker
import de.christianbernstein.packetier.Packet
import de.christianbernstein.packetier.broker
import de.christianbernstein.packetier.debug.PacketierDebuggingClient
import de.christianbernstein.packetier.engines.ktor.KtorEngine
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit = broker(KtorEngine).run {
    init(wait = false)

    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
        this.packetEngine.broadPub(
            Broker.PACKETIER_SERVER_ID, Packet(
            type = "TestPacket"
        )
        )
    }, 0, 5, TimeUnit.SECONDS)

    PacketierDebuggingClient("test") {}

}
