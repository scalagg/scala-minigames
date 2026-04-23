package gg.tropic.practice.games.damage

import gg.scala.game.extensions.cosmetics.CosmeticRegistry
import gg.scala.game.extensions.cosmetics.messagebundles.KillMessageBundleCosmeticCategory
import gg.scala.game.extensions.cosmetics.messagebundles.cosmetics.MessageBundle
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player

object DeathMessageStrategy
{
    private fun getMessageBundlePhrase(player: Player): String
    {
        val bundle = CosmeticRegistry
            .findRelatedTo(KillMessageBundleCosmeticCategory)
            .filter { like ->
                like.equipped(player)
            }
            .filterIsInstance<MessageBundle>()
            .flatMap { bundle -> bundle.phrases }

        if (bundle.isEmpty())
        {
            return "slain"
        }

        return "${CC.GOLD}${bundle.random()}${CC.GRAY}"
    }

    fun generate(
        killedBy: Player?,
        killedDisplayName: String,
        killedByDisplayName: String?,
        eliminationCause: EliminationCause,
        alternativeDeath: Boolean = false,
    ) = if (alternativeDeath && killedBy == null && eliminationCause == EliminationCause.VOID_DAMAGE)
    {

        "${CC.RED}$killedDisplayName${CC.GRAY} fell into the void!"
    } else
    {

        when (eliminationCause)
        {
            EliminationCause.KILLED_BY_PLAYER ->
            {
                val phrase = if (killedBy is Player)
                    getMessageBundlePhrase(killedBy) else "slain"

                "${CC.GREEN}$killedDisplayName${CC.GRAY} was $phrase by ${CC.RED}$killedByDisplayName${CC.GRAY}!"
            }

            else -> eliminationCause.formatMessage(
                killedDisplayName,
                killedByDisplayName
            )
        }
    }
}
