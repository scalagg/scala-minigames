package gg.tropic.practice.map.utilities

import com.cryptomorin.xseries.XMaterial
import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.metadata.sign.MapSignMetadataModel
import gg.tropic.practice.schematics.manipulation.BlockChanger
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.ServerVersion
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.util.Vector

data class MapMetadata(
    val metadataSignLocations: List<Vector>,
    val metadata: List<AbstractMapMetadata>,
    var synthetic: List<MapSignMetadataModel>?,
)
{
    fun composite(world: World) = synthetics(world) + metadata
    fun synthetics(world: World) = if (synthetic == null)
        listOf() else MapMetadataScanUtilities.buildJustInTimeMetadata(synthetic!!, world)

    fun clearSignLocations(world: World)
    {
        Schedulers
            .sync()
            .run {
                metadataSignLocations
                    .mapNotNull {
                        world.getBlockAt(it.blockX, it.blockY, it.blockZ)
                    }
                    .forEach {
                        val typeName = it.type.name
                        if (typeName.endsWith("_SIGN") || typeName.endsWith("_WALL_SIGN") || typeName.endsWith("_HANGING_SIGN"))
                        {
                            it.type = Material.AIR
                        }
                    }
            }
    }
}
