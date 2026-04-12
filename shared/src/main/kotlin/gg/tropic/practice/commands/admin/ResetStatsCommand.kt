package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.lemon.player.wrapper.AsyncLemonPlayer
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.statistics.TrackedKitStatistic
import gg.tropic.practice.statistics.StatisticService
import gg.tropic.practice.statistics.StatisticID
import gg.tropic.practice.statistics.statisticWrite
import gg.tropic.practice.statistics.statisticIds
import net.evilblock.cubed.util.CC
import org.bukkit.command.CommandSender

@AutoRegister
object ResetStatsCommand : ScalaCommand()
{
    private fun minigameTypeFor(kit: Kit): String? = if (kit.features(FeatureFlag.MiniGameType))
    {
        kit.featureConfig(FeatureFlag.MiniGameType, "id")
    }
    else
    {
        null
    }

    private fun minigameStatisticIDs(kit: Kit?): List<StatisticID>
    {
        val minigameTypeIDs = if (kit == null)
        {
            KitService.cached().kits.values
                .mapNotNull(::minigameTypeFor)
                .toSet()
        }
        else
        {
            minigameTypeFor(kit)
                ?.let(::setOf)
                ?: emptySet()
        }

        val minigameKitIDs = if (kit == null)
        {
            KitService.cached().kits.values
                .asSequence()
                .filter { it.features(FeatureFlag.MiniGameType) }
                .map(Kit::id)
                .toSet()
        }
        else
        {
            if (kit.features(FeatureFlag.MiniGameType)) setOf(kit.id)
            else emptySet()
        }

        if (minigameTypeIDs.isEmpty() && minigameKitIDs.isEmpty())
        {
            return emptyList()
        }

        return StatisticService
            .trackedStatistics()
            .asSequence()
            .map { statistic -> statistic.id }
            .filter { id ->
                if (id.isCustom())
                {
                    val custom = id.custom
                        ?: return@filter false

                    minigameTypeIDs.any { typeID ->
                        custom == typeID || custom.startsWith("$typeID:")
                    }
                }
                else
                {
                    val trackedKitID = id.kit
                        ?: return@filter false

                    trackedKitID in minigameKitIDs || trackedKitID in minigameTypeIDs
                }
            }
            .distinctBy(StatisticID::toId)
            .toList()
    }

    enum class StatReset(
        val resetFor: PracticeProfile.(Kit?) -> Unit
    )
    {
        ELO({ kit ->
            statisticWrite(
                statisticIds {
                    if (kit == null)
                    {
                        allServerKits()
                    } else
                    {
                        kits(kit)
                    }

                    ranked()
                    allLifetimes()
                    types(TrackedKitStatistic.ELO)
                }
            ) {
                update(defaultValue)
            }
        }),
        CasualStats({ kit ->
            statisticWrite(
                statisticIds {
                    if (kit == null)
                    {
                        allServerKits()
                    } else
                    {
                        kits(kit)
                    }

                    casual()
                    allLifetimes()
                    allTypes()
                }
            ) {
                update(defaultValue)
            }
        }),
        RankedStats({ kit ->
            statisticWrite(
                statisticIds {
                    if (kit == null)
                    {
                        allServerKits()
                    } else
                    {
                        kits(kit)
                    }

                    ranked()
                    allLifetimes()
                    allTypes()
                }
            ) {
                update(defaultValue)
            }
        }),
        GlobalStats({ _ ->
            statisticWrite(
                statisticIds {
                    globalKit()
                    globalQueueType()
                    allLifetimes()
                    allTypes()
                }
            ) {
                update(defaultValue)
            }
        }),
        MinigameStats({ kit ->
            val minigameStatisticIDs = minigameStatisticIDs(kit)
            if (minigameStatisticIDs.isNotEmpty())
            {
                statisticWrite(minigameStatisticIDs) {
                    update(defaultValue)
                }
            }
        }),
        AllStats({ kit ->
            RankedStats.resetFor(this, kit)
            GlobalStats.resetFor(this, kit)
            CasualStats.resetFor(this, kit)
            MinigameStats.resetFor(this, kit)
        })
    }

    @CommandAlias("resetstats")
    @CommandCompletion("@mip-players * @kits")
    @CommandPermission("practice.command.resetstats")
    fun onResetStats(
        player: CommandSender,
        target: AsyncLemonPlayer,
        resetOptions: StatReset,
        @Optional kit: Kit?
    ) = target.validatePlayers(player, false) {
        StatisticService
            .update(it.uniqueId) {
                resetOptions.resetFor(this, kit)
            }
            .thenRun {
                player.sendMessage(
                    "${CC.GREEN}You have reset the ${CC.BOLD}${
                        resetOptions.name
                    }${CC.GREEN} for ${CC.WHITE}${it.name}${CC.GREEN}${
                        if (kit != null) " on kit ${CC.PRI}${kit.id}${CC.GREEN}" else ""
                    }."
                )
            }
            .join()
    }
}
