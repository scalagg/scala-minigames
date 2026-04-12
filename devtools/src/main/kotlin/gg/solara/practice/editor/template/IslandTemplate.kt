package gg.solara.practice.editor.template

import gg.scala.commons.spatial.Position

data class IslandTemplate(
    val basePosition: Position,
    val chestDiffs: Position,
    val enderChestDiffs: Position,
    val shopDiffs: Position,
    val upgradeDiffs: Position,
    val bedBlockDiffs: List<Position>
)
