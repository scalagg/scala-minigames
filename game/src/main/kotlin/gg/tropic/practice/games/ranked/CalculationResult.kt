package gg.tropic.practice.games.ranked

data class CalculationResult(
    val winnerOld: Int,
    val winnerGain: Int,
    val loserOld: Int,
    val loserGain: Int
)
{
    val winnerNew = winnerOld + winnerGain
    val loserNew = loserOld + loserGain
}
