package gg.tropic.practice.map

import org.bukkit.World
import java.util.UUID

data class BuiltMapReplication(
    val associatedMap: Map,
    val world: World?,
    var scheduledForExpectedGame: UUID? = null,
    var inUse: Boolean = false
)
