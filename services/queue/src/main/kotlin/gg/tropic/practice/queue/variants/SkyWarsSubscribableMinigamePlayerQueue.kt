package gg.tropic.practice.queue.variants

import gg.scala.minigame.skywars.shared.SkyWarsGameConfiguration
import gg.scala.minigame.skywars.shared.SkyWarsMode
import gg.tropic.practice.application.api.defaults.defaults.kit.ImmutableKit
import gg.tropic.practice.queue.AbstractSubscribableMinigamePlayerQueue
import gg.tropic.practice.queue.QueueEntry

class SkyWarsSubscribableMinigamePlayerQueue(
    kit: ImmutableKit,
    private val mode: SkyWarsMode,
) : AbstractSubscribableMinigamePlayerQueue(
    miniGameMode = mode,
    kit = kit,
    queueType = mode.queueType,
    miniProvider = mode.providerVersion
)
{
    override fun constructConfigurationForInitiatorEntry(entry: QueueEntry) =
        SkyWarsGameConfiguration(mode = mode)
}
