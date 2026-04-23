package mc.arch.minigames.parties

import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.QuickAccess.username
import mc.arch.minigames.parties.service.NetworkPartyService
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import java.util.UUID

fun Player.toParty() = NetworkPartyService.findParty(uniqueId)
fun UUID.toDisplayName(): String {
    val colored = QuickAccess.coloredName(this)
    if (colored != null) return colored
    return "${CC.GRAY}${username()}"
}
