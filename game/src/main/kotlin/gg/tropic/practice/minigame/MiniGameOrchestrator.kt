package gg.tropic.practice.minigame

import gg.tropic.practice.expectation.GameExpectation
import gg.tropic.practice.kit.Kit
import org.bukkit.World

interface MiniGameOrchestrator<T : MiniGameConfiguration>
{
    val id: String

    fun construct(arenaWorld: World, kit: Kit, expectation: GameExpectation): AbstractMiniGameGameImpl<T>
}
