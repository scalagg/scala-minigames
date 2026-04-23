package gg.tropic.practice.queue

data class MinMaxRangedNumber(
    val med: Int, var diffsBy: Int
)
{
    fun toIntRangeInclusive() = (med - diffsBy)..(med + diffsBy)
}
