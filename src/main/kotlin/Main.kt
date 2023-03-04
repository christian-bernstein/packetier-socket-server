import de.christianbernstein.packetier.Packetier
import de.christianbernstein.packetier.engine.createProtocol
import de.christianbernstein.packetier.engine.events.SessionMessageReceivedEvent

fun main(args: Array<String>) {
    Packetier().run {

        this.packetEngine.createSession("a") {
            bus.register<SessionMessageReceivedEvent> {
                println("Session $id received a message")
            }
        }

        init()
    }
}
