import de.christianbernstein.packetier.Broker
import de.christianbernstein.packetier.debug.PacketierDebuggingClient
import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.events.SessionPacketReceivedEvent
import de.christianbernstein.packetier.event.Event

fun main(args: Array<String>) {
    Broker().run {
        init(wait = false)

        PacketierDebuggingClient("receiver", configurator = {
            skipTest = false
        }) {



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


            PacketierDebuggingClient("sender") {
                this.awaitPacket { it.type == "ActivationPacket" }
                this.send(Packet("test"))
            }
        }
    }
}

class TestEvent(
    val message: String? = "test"
): Event("TestEvent")
