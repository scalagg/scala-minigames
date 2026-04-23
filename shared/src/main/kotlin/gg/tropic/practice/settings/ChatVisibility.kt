package gg.tropic.practice.settings

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

enum class ChatVisibility : SettingValue
{
    Global,
    Match;

    override val displayName: String
        get() = name

    override fun display(player: Player) = true
}
