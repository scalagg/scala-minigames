package gg.tropic.practice.games.ranked

interface EloCalculator
{
    fun getNewRating(winner: Int, loser: Int): CalculationResult
}
