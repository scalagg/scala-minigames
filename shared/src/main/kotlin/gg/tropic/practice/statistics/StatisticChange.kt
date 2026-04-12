package gg.tropic.practice.statistics

data class StatisticChange(
    val id: String,
    val old: LeaderboardPosition,
    val new: LeaderboardPosition,
    val next: LeaderboardPosition?
)
{
    fun requiredScore() = next!!.score - new.score
}
