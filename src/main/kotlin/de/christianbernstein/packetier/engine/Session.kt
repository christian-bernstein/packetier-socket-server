package de.christianbernstein.packetier.engine

data class Session(
    val id: String,
    val engine: PacketEngine,
    var subscriber: PacketSubscriber,
    var onSessionClosed: () -> Unit = {},
    // Session-specific data
    var sessionCache: MutableMap<String, Any> = mutableMapOf()
) {

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

}
