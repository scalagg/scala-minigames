package gg.tropic.practice.map.metadata.scanner.impl

import gg.tropic.practice.map.metadata.impl.MapSpawnProtExpandMetadata
import gg.tropic.practice.map.metadata.scanner.AbstractMapMetadataScanner
import gg.tropic.practice.map.metadata.sign.MapSignMetadataModel
import org.bukkit.World

object MapSpawnProtExpandMetadataScanner : AbstractMapMetadataScanner<MapSpawnProtExpandMetadata>()
{
    override val type = "sprot"
    override fun scan(
        id: String,
        models: List<MapSignMetadataModel>,
        world: World
    ) = MapSpawnProtExpandMetadata(id)
}
