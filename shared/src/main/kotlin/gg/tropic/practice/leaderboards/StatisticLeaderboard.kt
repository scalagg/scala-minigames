package gg.tropic.practice.leaderboards

import gg.tropic.practice.kit.Kit

data class StatisticLeaderboard(
    val commonlyViewedLeaderboardType: CommonlyViewedLeaderboardType,
    val kit: Kit?
)
