package gg.tropic.practice.games.elo

data class ELOUpdates(
    val winner: Pair<Int, Int>,
    val loser: Pair<Int, Int>,
)
