package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.scala.commons.spatial.Position

data class MapSpawnMetadata(
    override val id: String,
    var position: Position
) : AbstractMapMetadata()
{
    override fun report() = "Spawn | $id | $position"
    override fun getAbstractType() = MapSpawnMetadata::class.java
}
