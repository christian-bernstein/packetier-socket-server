package de.christianbernstein.packetier

interface IEventListener<T: Event> {
    fun handle(event: T)
}
