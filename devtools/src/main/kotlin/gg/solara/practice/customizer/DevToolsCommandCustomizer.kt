package gg.solara.practice.customizer

import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.scala.minigame.skywars.shared.cages.SchemaCageCommandCustomizers

object DevToolsCommandCustomizer
{
    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager) = SchemaCageCommandCustomizers.customize(manager)
}
