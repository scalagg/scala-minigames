package gg.tropic.practice.friendship

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

enum class FriendshipStateSetting(newDisplayName: String? = null) : SettingValue
{
    Enabled,
    FriendsOnly("Friends Only"),
    Disabled;

    override val displayName = newDisplayName ?: name
    override fun display(player: Player) = true
}
