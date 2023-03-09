package de.christianbernstein.packetier

import de.christianbernstein.packetier.event.EventBus

@Suppress("unused")
data class Session(
    val id: String,
    val engine: PacketEngine,
    var subscriber: PacketSubscriber,
    var onSessionClosed: () -> Unit = {},
    // Session-specific data
    var sessionCache: MutableMap<String, Any> = mutableMapOf(),
    var publicToken: String,
    var privateToken: String
) {

    val bus: EventBus = EventBus()

    /**
     * Obtain a value from the cached property store.
     * Note: The cached store is not persistent
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedProperty(key: String, def: T? = null): T? {
        return (this.sessionCache[key] ?: return def) as T
    }

    /**
     * Set a value in the cached property store.
     * Note: The cached store is not persistent
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> setCachedProperty(key: String, value: T): T = value.also { this.sessionCache[key] = it }

    fun handleIncomingMessage(ctx: PacketSubscriberContext): Unit {
        // Pass message to pre-handler

        // Pass message to user-subscriber
        this.subscriber(ctx)
    }


}
