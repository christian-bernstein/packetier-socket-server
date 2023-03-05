package de.christianbernstein.packetier.socket

import de.christianbernstein.packetier.engine.PacketSubscriberContext

@Suppress("MemberVisibilityCanBePrivate")
open class Endpoint(val name: String, val handler: PacketSubscriberContext.() -> Unit = {}) {

    open fun handle(): PacketSubscriberContext.() -> Unit = this.handler

    /**
     * TODO: Rename function
     */
    fun pair(): Pair<String, PacketSubscriberContext.() -> Unit> = this.name to this.handle()

}
