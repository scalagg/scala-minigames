package gg.tropic.practice.parkour

import gg.scala.commons.spatial.Bounds
import gg.scala.commons.spatial.Position

data class ParkourConfiguration(
    var priorStart: Position? = null,
    var start: Bounds? = null,
    var checkpoints: MutableMap<Int, Bounds> = mutableMapOf(),
    var end: Bounds? = null
)
{
    fun isReady() = priorStart != null && start != null && end != null
}
