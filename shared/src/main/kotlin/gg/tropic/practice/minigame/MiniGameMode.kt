package gg.tropic.practice.minigame

interface MiniGameMode
{
    val teamSize: Int
    val teamCount: Int

    fun maxPlayers(): Int
}
