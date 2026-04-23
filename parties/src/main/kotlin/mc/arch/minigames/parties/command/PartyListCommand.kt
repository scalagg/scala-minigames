package mc.arch.minigames.parties.command

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.annotations.commands.HighPriority
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer

@AutoRegister
@HighPriority
object PartyListCommand : ScalaCommand()
{
    @CommandAlias("plist|pl|partylist")
    fun onPartyList(player: ScalaPlayer) = PartyCommand.onInfo(player.bukkit())
}
