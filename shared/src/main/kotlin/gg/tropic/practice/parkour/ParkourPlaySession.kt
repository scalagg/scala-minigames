package gg.tropic.practice.parkour

data class ParkourPlaySession(
    val start: Long = System.currentTimeMillis(),
    val checkpointTimes: MutableMap<Int, Long> = mutableMapOf()
)
