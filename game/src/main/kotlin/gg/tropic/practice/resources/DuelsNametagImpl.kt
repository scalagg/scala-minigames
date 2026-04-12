package gg.tropic.practice.resources

import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.extensions.RBTeamSide
import gg.tropic.practice.extensions.toRBTeamSide
import gg.tropic.practice.visuals.TeamVisuals
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.nametag.NametagProvider
import net.evilblock.cubed.nametag.NametagProviderRegister
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player

@NametagProviderRegister
object DuelsNametagImpl : NametagProvider("practice", Int.MAX_VALUE)
{
    private val ENEMY_TEAM_NAME_TAG = createNametag("${CC.RED}", "", 10_000)
    private val SELF_TEAM_NAME_TAG = createNametag("${CC.GREEN}", "", 9_000)

    private val RED_TEAM_NAME_TAG = createNametag("${CC.B_RED}R${CC.RED} ", "", 100000)
    private val GREEN_TEAM_NAME_TAG = createNametag("${CC.B_GREEN}G${CC.GREEN} ", "", 99999)
    private val BLUE_TEAM_NAME_TAG = createNametag("${CC.B_BLUE}B${CC.BLUE} ", "", 99999)

    private val SPECTATOR_NAME_TAG = createNametag("${CC.B_GRAY}S${CC.GRAY} ", "", -1)
    private val minigameTeamTags = mutableMapOf<String, NametagInfo>()

    private fun minigameTeamNametag(teamIdentifier: TeamIdentifier): NametagInfo
    {
        return minigameTeamTags.getOrPut(teamIdentifier.label) {
            val color = TeamVisuals.toChatColor(teamIdentifier)
            val readable = TeamVisuals.toReadable(color)
            val letter = readable.first().uppercaseChar()
            createNametag("$color[$letter] ", "", 90000 - teamIdentifier.priority())
        }
    }

    override fun fetchNametag(
        viewed: Player, viewer: Player
    ): NametagInfo?
    {
        val viewedGame = GameService
            .byPlayerOrSpectator(viewed.uniqueId)
            ?: return null

        val viewerGame = GameService
            .byPlayerOrSpectator(viewer.uniqueId)
            ?: return null

        if (viewerGame.expectation == viewedGame.expectation)
        {
            if (GameService.isSpectating(viewed))
            {
                return SPECTATOR_NAME_TAG
            }

            if (viewedGame.miniGameLifecycle != null)
            {
                val nametag = viewedGame.miniGameLifecycle?.provideNametagFor(viewed, viewer)
                if (nametag != null)
                {
                    return nametag
                }

                if (viewedGame.miniGameLifecycle?.typeConfiguration?.internalId == "bedwars")
                {
                    val team = viewedGame.getNullableTeam(viewed)
                        ?: return SPECTATOR_NAME_TAG

                    return minigameTeamNametag(team.teamIdentifier)
                }
            }

            if (viewedGame.flag(FeatureFlag.RedBlueTeams))
            {
                val viewedSide = viewedGame.getNullableTeam(viewed)
                    ?: return SPECTATOR_NAME_TAG

                val viewedRBSide = viewedSide
                    .teamIdentifier
                    .toRBTeamSide()

                return if (viewedRBSide == RBTeamSide.Red)
                    RED_TEAM_NAME_TAG else BLUE_TEAM_NAME_TAG
            }

            val viewedTeam = viewedGame.getTeamOf(viewed)
            val viewerTeam = viewedGame.getNullableTeam(viewer)
            if (!viewedGame.shouldContainIdentifiableTeams)
            {
                return if (viewedTeam.teamIdentifier == viewerTeam?.teamIdentifier)
                    SELF_TEAM_NAME_TAG else ENEMY_TEAM_NAME_TAG
            }

            return if (viewedTeam.teamIdentifier == TeamIdentifier.A)
                GREEN_TEAM_NAME_TAG else RED_TEAM_NAME_TAG
        }

        return null
    }
}
