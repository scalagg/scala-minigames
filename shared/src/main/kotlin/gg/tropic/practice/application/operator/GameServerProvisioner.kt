package gg.tropic.practice.application.operator

/**
 * @author Limo
 * @date 2/15/2026
 */
interface GameServerProvisioner
{

    fun isAvailable(): Boolean

    fun provision(gameType: String): String?

    fun isDraining(serverId: String): Boolean

    companion object
    {
        @Volatile
        var instance: GameServerProvisioner? = null
    }
}
