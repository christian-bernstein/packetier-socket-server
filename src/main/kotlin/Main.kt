import de.christianbernstein.packetier.debug.PacketierDebuggingClient
import de.christianbernstein.packetier.Packet
import de.christianbernstein.packetier.broker
import de.christianbernstein.packetier.events.SessionPacketReceivedEvent
import de.christianbernstein.packetier.Event

fun main(args: Array<String>) {
    broker().run {
        init(wait = false)

        val (sessionID, publicToken) = packetEngine.createSession {
            setCachedProperty("target", "You got it!")

            bus.register<SessionPacketReceivedEvent> {
                println("Session $id received a message")
            }
        }.let { listOf(it.id, it.publicToken) }

        println("Session id = '$sessionID'")

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
