package gg.tropic.practice.queue.variants

import gg.scala.minigame.bedwars.shared.BedWarsGameConfiguration
import gg.scala.minigame.bedwars.shared.BedWarsMode
import gg.tropic.practice.application.api.defaults.defaults.kit.ImmutableKit
import gg.tropic.practice.queue.AbstractSubscribableMinigamePlayerQueue
import gg.tropic.practice.queue.QueueEntry

class BedWarsSubscribableMinigamePlayerQueue(
    kit: ImmutableKit,
    private val mode: BedWarsMode
) : AbstractSubscribableMinigamePlayerQueue(
    miniGameMode = mode,
    kit = kit,
    queueType = mode.queueType,
    miniProvider = mode.providerVersion
)
{
    override fun constructConfigurationForInitiatorEntry(entry: QueueEntry) =
        BedWarsGameConfiguration(mode = mode)
}
