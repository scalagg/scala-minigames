package mc.arch.lobby.main

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.core.plugin.*

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
