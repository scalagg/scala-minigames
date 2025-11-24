package mc.arch.lobby.main

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.core.plugin.*

/**
 * @author Subham
 * @since 6/27/25
 */
@Plugin(
    name = "Lobby",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthor("ArchMC")
@PluginWebsite("https://arch.mc")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("Minigames"),
)
class MainLobby : ExtendedScalaPlugin()
