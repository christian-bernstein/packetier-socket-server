import de.christianbernstein.packetier.Packetier
import de.christianbernstein.packetier.debug.PacketierDebuggingClient
import de.christianbernstein.packetier.engine.Packet
import de.christianbernstein.packetier.engine.events.SessionMessageReceivedEvent

fun main(args: Array<String>) {
    Packetier().run {
        init(wait = false)

        PacketierDebuggingClient("receiver") {
            log("User suite started")

            try {
                this@run.packetEngine.getSession("receiver").bus.register<SessionMessageReceivedEvent> {
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
