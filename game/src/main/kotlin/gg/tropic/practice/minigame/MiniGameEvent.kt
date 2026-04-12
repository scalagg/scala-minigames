package gg.tropic.practice.minigame

import java.time.Duration

interface MiniGameEvent
{
    val description: String
    val duration: Duration

    fun execute()
    {

    }
}
