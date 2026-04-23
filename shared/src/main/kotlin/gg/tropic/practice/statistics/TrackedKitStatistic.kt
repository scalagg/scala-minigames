package gg.tropic.practice.statistics

enum class TrackedKitStatistic(val timeSensitive: List<StatisticLifetime> = emptyList())
{
    Plays, Losses, Kills, Deaths,
    WinStreak(listOf(StatisticLifetime.Daily)),
    WinStreakHighest,
    ELO,
    Wins(listOf(StatisticLifetime.Daily, StatisticLifetime.Weekly, StatisticLifetime.Monthly))
}
