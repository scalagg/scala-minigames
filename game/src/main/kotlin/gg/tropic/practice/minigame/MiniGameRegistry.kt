package gg.tropic.practice.minigame

object MiniGameRegistry
{
    val miniGameOrchestrators = mutableMapOf<String, MiniGameOrchestrator<*>>()
}
