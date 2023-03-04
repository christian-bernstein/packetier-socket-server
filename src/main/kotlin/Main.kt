import de.christianbernstein.packetier.Packetier
import de.christianbernstein.packetier.engine.createProtocol

fun main(args: Array<String>) {
    Packetier().run {

        this.packetEngine.createSession("a") {

        }

        init()
    }
}
