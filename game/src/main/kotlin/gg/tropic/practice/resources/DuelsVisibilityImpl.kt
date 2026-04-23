package gg.tropic.practice.resources

import gg.tropic.practice.games.GameService
import gg.tropic.practice.settings.isASilentSpectator
import gg.tropic.practice.ugc.toHostedWorld
import net.evilblock.cubed.visibility.VirtualVisibility
import net.evilblock.cubed.visibility.VisibilityAction
import net.evilblock.cubed.visibility.VisibilityAdapter
import net.evilblock.cubed.visibility.VisibilityAdapterRegister
import org.bukkit.entity.Player

@VisibilityAdapterRegister("duels")
object DuelsVisibilityImpl : VisibilityAdapter
{
    override fun getVirtualAppearanceBetween(viewer: Player, viewed: Player): VirtualVisibility
    {
        val hostedWorldPlayer = viewer.toHostedWorld()
        val hostedWorldTarget = viewed.toHostedWorld()

        val playerGame = GameService.byPlayerOrSpectator(viewer.uniqueId)
        val targetGame = GameService.byPlayerOrSpectator(viewed.uniqueId)

        val eligibleForSilentSpectator = !(playerGame != null && targetGame?.expectation == playerGame.expectation) ||
            playerGame.expectedSpectators.contains(viewed.uniqueId)

        if (
            (playerGame != null && targetGame?.expectation == playerGame.expectation && playerGame.miniGameLifecycle != null) || 
            (hostedWorldPlayer != null && hostedWorldPlayer.globalId == hostedWorldTarget?.globalId) 
        )
        {
            val isTargetSpectating = GameService.isSpectating(viewed)
            val isViewerSpectating = GameService.isSpectating(viewer)

            if (isTargetSpectating)
            {
                if (!isViewerSpectating) 
                {
                    if (playerGame != null && playerGame.shouldShowAllPlayers)
                    {
                        if (viewed.isASilentSpectator() && eligibleForSilentSpectator)
                        {
                            return VirtualVisibility.HIDDEN
                        }

                        return VirtualVisibility.NEUTRAL
                    }

                    return VirtualVisibility.HIDDEN
                }

                if (viewed.isASilentSpectator() && eligibleForSilentSpectator) 
                {
                    return VirtualVisibility.HIDDEN
                }
            }

            return VirtualVisibility.NEUTRAL
        }

        if (playerGame == null && targetGame == null && hostedWorldPlayer == null && hostedWorldTarget == null)
        {
            return VirtualVisibility.NEUTRAL
        }

        return VirtualVisibility.HIDDEN
    }

    override fun getAction(
        toRefresh: Player, refreshFor: Player
    ): VisibilityAction
    {
        val hostedWorldPlayer = refreshFor.toHostedWorld()
        val hostedWorldTarget = toRefresh.toHostedWorld()

        val playerGame = GameService.byPlayerOrSpectator(refreshFor.uniqueId)
        val targetGame = GameService.byPlayerOrSpectator(toRefresh.uniqueId)

        val eligibleForSilentSpectator = !(playerGame != null && targetGame?.expectation == playerGame.expectation) ||
            playerGame.expectedSpectators.contains(toRefresh.uniqueId)

        if (
            (playerGame != null && targetGame?.expectation == playerGame.expectation) ||
            (hostedWorldPlayer != null && hostedWorldPlayer.globalId == hostedWorldTarget?.globalId)
        )
        {
            val isTargetSpectating = GameService.isSpectating(toRefresh)
            val isViewerSpectating = GameService.isSpectating(refreshFor)

            if (isTargetSpectating)
            {
                if (!isViewerSpectating) 
                {
                    if (playerGame != null && playerGame.shouldShowAllPlayers)
                    {
                        if (toRefresh.isASilentSpectator() && eligibleForSilentSpectator)
                        {
                            return VisibilityAction.HIDE
                        }

                        return VisibilityAction.NEUTRAL
                    }

                    return VisibilityAction.HIDE
                }

                if (toRefresh.isASilentSpectator() && eligibleForSilentSpectator)
                {
                    return VisibilityAction.HIDE
                }
            }

            return VisibilityAction.NEUTRAL
        }

        return VisibilityAction.HIDE
    }
}
