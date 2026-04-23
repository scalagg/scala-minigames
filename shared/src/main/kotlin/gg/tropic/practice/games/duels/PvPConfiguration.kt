package gg.tropic.practice.games.duels

data class PvPConfiguration(
    val blockHitLimit: Int = 0,
    val runningAndEating: Boolean = false,
    val criticalHitLimit: Int = 0
)
