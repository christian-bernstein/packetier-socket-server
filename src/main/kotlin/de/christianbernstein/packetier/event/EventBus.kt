package de.christianbernstein.packetier.event

import kotlin.reflect.KClass

class EventBus {

    val listeners: MutableMap<KClass<*>, MutableList<IEventListener<out Event>>> = mutableMapOf()

    inline infix fun <reified T : Event> register(listener: IEventListener<T>) {
        val eventClass = T::class

        // TODO: Use logger
        println("Adding listener to Event-class '${eventClass.qualifiedName}'")

        val eventListeners: MutableList<IEventListener<out Event>> = listeners.getOrPut(eventClass) { mutableListOf() }
        eventListeners.add(listener)
    }

    inline infix fun <reified T: Event> fire(event: T) = listeners[event::class]

        // TODO: Remove
        ?.also { println("listeners: [$it]") }

        ?.asSequence()
        ?.filterIsInstance<IEventListener<T>>()
        ?.forEach { it.handle(event) }

    fun fireUnknown(event: Event) = listeners[event::class]
        ?.asSequence()
        ?.filterIsInstance<IEventListener<Event>>()
        ?.forEach { it.handle(event) }

    inline fun <reified T : Event> register(crossinline handler: T.() -> Unit) = this.register(object : IEventListener<T> {
        override fun handle(event: T) = event.handler()
    })

    // TODO: Remove
    @Deprecated("No more operator stuff..", ReplaceWith("this.register(handler)"))
    inline operator fun <reified T : Event> plus(listener: IEventListener<T>) = this.register(listener)

    // TODO: Remove
    @Deprecated("No more operator stuff..", ReplaceWith("this.register(handler)"))
    inline operator fun <reified T : Event> plus(crossinline listener: (event: T) -> Unit) = this.register(object : IEventListener<T> {
        override fun handle(event: T) = listener(event)
    })

    // TODO: Maybe remove
    @Deprecated("No more operator stuff..", ReplaceWith("this.register(handler)"))
    @JvmName("unknown_add")
    inline operator fun plus(crossinline listener: (event: Event) -> Unit) = this.register(object :
        IEventListener<Event> {
        override fun handle(event: Event) = listener(event)
    })
}
