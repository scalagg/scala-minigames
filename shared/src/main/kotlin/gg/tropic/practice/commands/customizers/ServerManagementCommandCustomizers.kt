package gg.tropic.practice.commands.customizers

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager

/**
 * @author Limo
 * @date 2/15/2026
 */
object ServerManagementCommandCustomizers
{
    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandCompletions
            .registerAsyncCompletion("sm-servers") {
                ServerContainer
                    .allServers<GameServer>()
                    .map { it.id }
                    .sorted()
            }
    }
}
