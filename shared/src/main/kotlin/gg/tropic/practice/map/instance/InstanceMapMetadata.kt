package gg.tropic.practice.map.instance

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.utilities.MapMetadata
import org.bukkit.World

class InstanceMapMetadata(
    val mapMetadata: MapMetadata,
    val metadata: List<AbstractMapMetadata>
)
{
    fun composite(world: World) = metadata
    fun synthetics(world: World) = mapMetadata.synthetics(world)

    fun clearSignLocations(world: World)
    {
        mapMetadata.clearSignLocations(world)
    }
}
