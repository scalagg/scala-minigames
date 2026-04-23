package gg.tropic.practice.minigame.spectators

import org.bukkit.entity.Player

data class PlayerParticipant(
    val player: Player,
    var displayName: String,
    var description: List<String>
)
