package gg.scala.minigames.persistent.prisons.game

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.core.plugin.Plugin
import gg.scala.commons.core.plugin.PluginAuthor
import gg.scala.commons.core.plugin.PluginAuthorComposite
import gg.scala.commons.core.plugin.PluginDependency
import gg.scala.commons.core.plugin.PluginDependencyComposite
import gg.scala.commons.core.plugin.PluginWebsite

/**
 * @author Limo
 * @date 4/18/2026
 */
@Plugin(
    name = "Moonstone",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthorComposite(
    PluginAuthor("Limo"),
)
@PluginWebsite("https://scala.gg")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("Minigames"),
)
class PersistentGamePrisons : ExtendedScalaPlugin()
{
    companion object
    {
        lateinit var instance: PersistentGamePrisons
    }


    @ContainerEnable
    fun containerEnable()
    {
        instance = this
    }
}
