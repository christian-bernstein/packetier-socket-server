import de.christianbernstein.packetier.Broker
import de.christianbernstein.packetier.debug.PacketierDebuggingClient
import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.events.SessionPacketReceivedEvent
import de.christianbernstein.packetier.event.Event

fun main(args: Array<String>) {
    Broker().run {
        init(wait = false)

        val (sessionID, publicToken) = packetEngine.createSession {
            setCachedProperty("target", "You got it!")

            bus.register<SessionPacketReceivedEvent> {
                println("Session $id received a message")
            }
        }.let { listOf(it.id, it.publicToken) }

        PacketierDebuggingClient("attachment-test") {
            attachToSession(sessionID, publicToken)
        }





        PacketierDebuggingClient("receiver", configurator = { skipTest = true }) {
            // val connectionID = awaitPacket { it.type == "ActivationPacket" }.getString("internalSocketConnectionID")
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
