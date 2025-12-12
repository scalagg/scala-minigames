package gg.tropic.practice.queue

import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.UUID

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
data class QueueState(
    val kitId: String?,
    val queueType: QueueType,
    val teamSize: Int,
    val entry: QueueEntry
)

fun UUID.toQueueState() = ScalaCommonsSpigot.instance.kvConnection
    .sync()
    .hget(
        "$queueV2Namespace:states",
        this.toString()
    )
    ?.let { Serializers.gson.fromJson(it, QueueState::class.java) }

fun totalPlayersQueued() = ScalaCommonsSpigot.instance.kvConnection
    .sync()
    .hlen("$queueV2Namespace:states")
