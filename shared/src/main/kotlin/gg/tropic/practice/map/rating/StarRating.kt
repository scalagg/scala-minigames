package gg.tropic.practice.map.rating

import net.evilblock.cubed.util.CC
import org.bukkit.ChatColor

enum class StarRating(val color: ChatColor)
{
    One(ChatColor.RED), Two(ChatColor.GOLD), Three(ChatColor.YELLOW), Four(ChatColor.YELLOW), Five(ChatColor.GREEN);

    val format = "$color[${ordinal + 1}✮]${CC.RESET}"
}
