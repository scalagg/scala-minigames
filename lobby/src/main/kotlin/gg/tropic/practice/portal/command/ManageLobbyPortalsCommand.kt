package gg.tropic.practice.portal.command

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.portal.menu.ViewLobbyPortalsMenu

@AutoRegister
@CommandAlias("managelobbyportals")
@CommandPermission("practice.command.managelobbyportals")
object ManageLobbyPortalsCommand : ScalaCommand()
{
    @Default
    fun onDefault(player: ScalaPlayer) = ViewLobbyPortalsMenu().openMenu(player.bukkit())
}
