package gg.tropic.practice.games.loadout

import org.bukkit.entity.Player

interface SelectedLoadout
{
    fun displayName(): String
    fun apply(player: Player)
}
