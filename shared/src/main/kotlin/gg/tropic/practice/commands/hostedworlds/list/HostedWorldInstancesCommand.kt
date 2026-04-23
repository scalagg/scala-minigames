package gg.tropic.practice.commands.hostedworlds.list

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.basics.plugin.commands.bukkit

@AutoRegister
object HostedWorldInstancesCommand : ScalaCommand()
{
    @CommandAlias("hostedworldinstances|hwis|hwilist")
    @CommandPermission("practice.command.hostedworldinstances")
    fun onHostedWorldInstanceList(player: ScalaPlayer) = HostedWorldInstancesMenu().openMenu(player.bukkit())
}
