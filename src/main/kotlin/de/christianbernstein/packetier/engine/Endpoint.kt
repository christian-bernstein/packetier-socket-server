package de.christianbernstein.packetier.engine

@Suppress("MemberVisibilityCanBePrivate")
open class Endpoint(val name: String, val handler: PacketSubscriberContext.() -> Unit = {}) {

    open fun handle(): PacketSubscriberContext.() -> Unit = this.handler

    /**
     * TODO: Rename function
     */
    fun pair(): Pair<String, PacketSubscriberContext.() -> Unit> = this.name to this.handle()

}
