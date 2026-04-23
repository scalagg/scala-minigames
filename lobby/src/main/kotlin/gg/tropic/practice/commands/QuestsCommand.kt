package gg.tropic.practice.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.command.ScalaCommand
import gg.scala.basics.plugin.commands.bukkit
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.menu.quests.PlayerFacingQuestsMenu

object QuestsCommand : ScalaCommand()
{
    @CommandAlias("quests|quest")
    fun onQuests(player: ScalaPlayer) = PlayerFacingQuestsMenu().openMenu(player.bukkit())
}
