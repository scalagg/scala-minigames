package gg.tropic.practice.queue

import gg.tropic.practice.persistence.RedisShared
import net.evilblock.cubed.serializers.Serializers
import java.util.*

interface SubscribablePlayerQueue
{
    val id: String
    val internalQueue: InternalQueue<QueueEntry>

    fun toQueueIDComponents() = id.toQueueIDComponents()

    fun onProcess(): List<QueueEntry>

    fun processAndDestroy()
    {
        runCatching { onProcess() }
            .onFailure {
                it.printStackTrace()
            }
            .getOrNull()
            ?.forEach {
                unsubscribe(it)
            }
    }

    fun playersInQueue() = internalQueue.listElements()

    fun isQueued(player: UUID) = internalQueue.contains(player)
    fun subscribe(entry: QueueEntry)
    {
        internalQueue.add(
            InternalQueueEntry(
                id = entry.leader,
                data = entry
            )
        )
        updateStates(entry)
    }

    fun unsubscribe(entry: QueueEntry)
    {
        internalQueue.remove(entry.leader)
        removeStates(entry)
    }

    fun removeStates(entry: QueueEntry)
    {
        entry.players.forEach { member ->
            runCatching {
                RedisShared.keyValueCache
                    .sync()
                    .hdel(
                        "$queueV2Namespace:states",
                        member.toString()
                    )
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }

    fun updateStates(entry: QueueEntry)
    {
        val components = id.toQueueIDComponents()
            ?: throw IllegalStateException(
                "$id is not a valid QueueID"
            )

        val queueState = QueueState(
            kitId = components.kitID,
            queueType = components.queueType,
            teamSize = components.teamSize,
            entry = entry
        )

        queueState.entry.players.forEach { member ->
            runCatching {
                RedisShared.keyValueCache
                    .sync()
                    .hset(
                        "$queueV2Namespace:states",
                        member.toString(),
                        Serializers.gson.toJson(queueState)
                    )
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }
}
