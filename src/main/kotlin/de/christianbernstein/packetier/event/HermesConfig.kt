package de.christianbernstein.packetier.event

import de.christianbernstein.packetier.event.EventBus
import de.christianbernstein.packetier.event.Hermes

data class HermesConfig(
    val defaultBusFactory: (busID: String, hermes: Hermes) -> EventBus = { _, _ -> EventBus() }
)
