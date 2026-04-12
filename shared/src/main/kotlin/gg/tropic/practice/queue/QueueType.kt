package gg.tropic.practice.queue

enum class QueueType(
    val coinMultiplier: Double
)
{
    Casual(1.0),
    Ranked(1.5),
    Robot(0.0)
}
