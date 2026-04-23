package gg.tropic.practice.settings.scoreboard

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

enum class ScoreboardStyle : SettingValue
{
    Default, Legacy, Disabled;

    override val displayName: String
        get() = name.lowercase().capitalize()

    override fun display(player: Player) = true
}

