package gg.tropic.practice.statistics

import net.evilblock.cubed.ScalaCommonsSpigot
import java.util.UUID

/**
 * @author Subham
 * @since 6/18/25
 */
object StatisticRedis
{
    fun scoreAndPositionOf(player: UUID, id: String, reversed: Boolean) = ScalaCommonsSpigot.instance
        .kvConnection
        .sync()
        .let {
            if (reversed)
            {
                it.zrevrankWithScore(
                    id,
                    player.toString()
                )
            } else
            {
                it.zrankWithScore(
                    id,
                    player.toString()
                )
            }
        }

    fun save(player: UUID, id: String, value: Long) = ScalaCommonsSpigot.instance
        .kvConnection
        .sync()
        .zadd(
            id,
            value.toDouble(),
            player.toString()
        )

    fun exists(id: String) = ScalaCommonsSpigot.instance
        .kvConnection
        .sync()
        .exists(id)

    fun incr(player: UUID, id: String, by: Long) = ScalaCommonsSpigot.instance
        .kvConnection
        .sync()
        .zincrby(
            id,
            by.toDouble(),
            player.toString()
        )

    fun save(player: UUID, id: String, value: Long, lifetime: StatisticLifetime)
    {
        val redis = ScalaCommonsSpigot.instance.kvConnection.sync()
        redis.zadd(id, value.toDouble(), player.toString())
        redis.expireat(id, lifetime.getTimeAtReset())
    }

    fun incr(player: UUID, id: String, by: Long, lifetime: StatisticLifetime)
    {
        val redis = ScalaCommonsSpigot.instance.kvConnection.sync()
        redis.zincrby(id, by.toDouble(), player.toString())
        redis.expireat(id, lifetime.getTimeAtReset())
    }

    fun getTopPlayers(id: String, limit: Int = 10, reversed: Boolean = true) = ScalaCommonsSpigot.instance
        .kvConnection
        .sync()
        .let { sync ->
            if (reversed)
            {
                sync.zrevrangeWithScores(
                    id,
                    0,
                    (limit - 1).toLong()
                )
            } else
            {
                sync.zrangeWithScores(
                    id,
                    0,
                    (limit - 1).toLong()
                )
            }
        }
}
