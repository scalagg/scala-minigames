package gg.tropic.practice.queue.variants

import gg.scala.minigame.bedwars.shared.BedWarsGameConfiguration
import gg.scala.minigame.bedwars.shared.BedWarsMode
import gg.tropic.practice.application.api.defaults.defaults.kit.ImmutableKit
import gg.tropic.practice.queue.AbstractSubscribableMinigamePlayerQueue
import gg.tropic.practice.queue.QueueEntry
import gg.tropic.practice.queue.QueueType

/**
 * @author Subham
 * @since 6/15/25
 */
class BedWarsSubscribableMinigamePlayerQueue(
    kit: ImmutableKit,
    private val mode: BedWarsMode
) : AbstractSubscribableMinigamePlayerQueue(
    miniGameMode = mode,
    kit = kit,
    queueType = QueueType.Casual // No support for ranked yet
)
{
    override fun constructConfigurationForInitiatorEntry(entry: QueueEntry) =
        BedWarsGameConfiguration(mode = mode)
}
