package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.scala.commons.spatial.Position

data class MapWitherMetadata(
    override val id: String,
    var position: Position
) : AbstractMapMetadata()
{
    override fun report() = "Wither | $id | $position"
    override fun getAbstractType() = MapWitherMetadata::class.java
}
