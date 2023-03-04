import de.christianbernstein.packetier.Packetier
import de.christianbernstein.packetier.debug.PacketierDebuggingClient
import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.events.SessionPacketReceivedEvent

fun main(args: Array<String>) {
    Packetier().run {
        init(wait = false)

        PacketierDebuggingClient("receiver") {
            log("User suite started")

            val connectionID = awaitPacket { it.type == "ActivationPacket" }.getString("internalSocketConnectionID")

            log("ActivationPacket received")

            try {
                this@run.packetEngine.getSession(connectionID).bus.register<SessionPacketReceivedEvent> {
                    println("Session $id received a message")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            log("Sending test packet")
            packetEngine.broadPub("sender", Packet("test"))

            log("User suite finished")
        }
    }
}
