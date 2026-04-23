package gg.tropic.practice.feature

import gg.scala.commons.annotations.plugin.SoftDependency
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan

@Service
@IgnoreAutoScan
@SoftDependency("Apollo-Bukkit")
object ApolloFeature
{
    @Configure
    fun configure()
    {
        try {
            val apolloClass = Class.forName("com.lunarclient.apollo.Apollo")
            val combatClass = Class.forName("com.lunarclient.apollo.module.combat.CombatModule")
            val moduleManagerMethod = apolloClass.getMethod("getModuleManager")
            val moduleManager = moduleManagerMethod.invoke(null)
            val getModuleMethod = moduleManager.javaClass.getMethod("getModule", java.lang.Class::class.java)
            val combatModule = getModuleMethod.invoke(moduleManager, combatClass)
            val disableMissPenalty = combatClass.getField("DISABLE_MISS_PENALTY").get(null)
            val getOptionsMethod = combatModule.javaClass.getMethod("getOptions")
            val options = getOptionsMethod.invoke(combatModule)
            val setMethod = options.javaClass.methods.firstOrNull { it.name == "set" && it.parameterTypes.size == 2 } ?: return
            setMethod.invoke(options, disableMissPenalty, java.lang.Boolean.TRUE)
        } catch (_: Throwable) {
        }
    }
}
