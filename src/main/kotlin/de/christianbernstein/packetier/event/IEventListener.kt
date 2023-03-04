package de.christianbernstein.packetier.event

interface IEventListener<T: Event> {
    fun handle(event: T)
}
