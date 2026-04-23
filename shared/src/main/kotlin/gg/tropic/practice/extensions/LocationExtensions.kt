package gg.tropic.practice.extensions

import gg.scala.commons.spatial.Position
import org.bukkit.Location

fun Location.toPositionCentered() = Position(
    x = x + 0.5,
    y = y,
    z = z + 0.5,
    yaw = yaw,
    pitch = pitch
)
