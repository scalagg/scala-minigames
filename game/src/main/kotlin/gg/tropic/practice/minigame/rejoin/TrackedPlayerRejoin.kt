package gg.tropic.practice.minigame.rejoin

import gg.tropic.practice.games.team.TeamIdentifier

data class TrackedPlayerRejoin(
    val previousTeam: TeamIdentifier,
    val joinTime: Long
)
