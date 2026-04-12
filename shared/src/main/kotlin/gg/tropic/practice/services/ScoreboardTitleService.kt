package gg.tropic.practice.services

import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.commons.scoreboard.TextAnimator
import gg.scala.commons.scoreboard.animations.TextFadeAnimation
import gg.scala.flavor.service.Close
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import net.evilblock.cubed.util.CC
import org.bukkit.ChatColor

@Service
object ScoreboardTitleService
{
    private val titleAnimator = TextAnimator.of(
        TextFadeAnimation(
            "${CC.BOLD}Duels",
            ChatColor.YELLOW,
            ChatColor.DARK_GRAY,
            ChatColor.GREEN
        )
    )

    private var scheduled = false

    fun getCurrentTitle() = "${titleAnimator.current().text}${
        if ("mipdev" in ServerSync.getLocalGameServer().groups) " ${CC.WHITE}(dev)" else ""
    }"

    @Configure
    fun configure()
    {
        titleAnimator.schedule()
        scheduled = true
    }

    @Close
    fun close()
    {
        if (scheduled)
        {
            titleAnimator.dispose()
            scheduled = false
        }
    }
}
