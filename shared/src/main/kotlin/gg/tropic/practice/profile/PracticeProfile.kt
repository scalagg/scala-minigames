package gg.tropic.practice.profile

import gg.scala.commons.graduation.Progressive
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.storable.IDataStoreObject
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.profile.loadout.Loadout
import gg.tropic.practice.profile.ranked.RankedBan
import gg.tropic.practice.statistics.Statistic
import gg.tropic.practice.statistics.StatisticID
import gg.tropic.practice.statistics.StatisticService
import io.lettuce.core.ScoredValue
import net.evilblock.cubed.util.time.Duration
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class PracticeProfile(
    override val identifier: UUID,
    override var matured: Set<String>? = setOf()
) : IDataStoreObject, Progressive
{
    private var rankedBan: RankedBan? = null
    var favoriteKits: MutableList<String> = mutableListOf()

    @Transient
    var immutableMode: Boolean? = null

    fun inImmutableMode() = immutableMode == true

    @Transient
    var cachedStatistics: MutableMap<String, ScoredValue<Long>>? = ConcurrentHashMap()

    @Transient
    var trackedStatisticChangeSet: MutableSet<String> = mutableSetOf()

    @Transient
    var trackedStatisticChangeMutations: MutableMap<Statistic, Statistic.() -> Unit> = mutableMapOf()

    fun track(statistic: String)
    {
        trackedStatisticChangeSet += statistic
    }

    fun track(statistic: Statistic, use: Statistic.() -> Unit)
    {
        trackedStatisticChangeMutations[statistic] = use
    }

    fun getStatisticValue(id: String) = cachedStatistics?.get(id)
        ?: StatisticService.statisticBy(id)?.scoreAndPosition()
            ?.apply {
                if (cachedStatistics == null)
                {
                    cachedStatistics = ConcurrentHashMap()
                }

                cachedStatistics!![id] = this
            }

    fun getStatisticValue(id: StatisticID) = getStatisticValue(id.toId())

    private var statistics: ConcurrentHashMap<String, Long>? = ConcurrentHashMap<String, Long>()
    fun statistics(): ConcurrentHashMap<String, Long>
    {
        if (statistics == null)
        {
            statistics = ConcurrentHashMap<String, Long>()
        }

        return statistics!!
    }

    val customLoadouts = ConcurrentHashMap<String, MutableList<Loadout>>()
    private var selectedDuelsCosmetics: ConcurrentHashMap<String, String>? = ConcurrentHashMap()
    private var ownedDuelsCosmetics: ConcurrentHashMap<String, MutableSet<String>>? = ConcurrentHashMap()

    private var spectatorPreferences: ConcurrentHashMap<String, Boolean>? = ConcurrentHashMap()

    private fun spectatorPreferences(): ConcurrentHashMap<String, Boolean>
    {
        if (spectatorPreferences == null)
        {
            spectatorPreferences = ConcurrentHashMap()
        }

        return spectatorPreferences!!
    }

    fun spectatorPreference(key: String, defaultValue: Boolean) = spectatorPreferences()[key] ?: defaultValue

    fun setSpectatorPreference(key: String, value: Boolean)
    {
        spectatorPreferences()[key] = value
    }

    private var leaderboardPreferences: ConcurrentHashMap<String, String>? = ConcurrentHashMap()

    private fun leaderboardPreferences(): ConcurrentHashMap<String, String>
    {
        if (leaderboardPreferences == null)
        {
            leaderboardPreferences = ConcurrentHashMap()
        }

        return leaderboardPreferences!!
    }

    fun leaderboardPreference(key: String, defaultValue: String) = leaderboardPreferences()[key] ?: defaultValue

    fun setLeaderboardPreference(key: String, value: String)
    {
        leaderboardPreferences()[key] = value
    }

    fun getLoadoutsFromKit(kit: Kit) = customLoadouts[kit.id] ?: mutableListOf()

    fun selectedDuelsCosmetic(groupId: String) = selectedDuelsCosmetics()[groupId] ?: "none"

    fun selectDuelsCosmetic(groupId: String, id: String)
    {
        selectedDuelsCosmetics()[groupId] = id
    }

    fun unlockedDuelsCosmetics(groupId: String): MutableSet<String>
    {
        return ownedDuelsCosmetics().getOrPut(groupId) { mutableSetOf("none") }
    }

    private fun selectedDuelsCosmetics(): ConcurrentHashMap<String, String>
    {
        if (selectedDuelsCosmetics == null)
        {
            selectedDuelsCosmetics = ConcurrentHashMap()
        }

        return selectedDuelsCosmetics!!
    }

    private fun ownedDuelsCosmetics(): ConcurrentHashMap<String, MutableSet<String>>
    {
        if (ownedDuelsCosmetics == null)
        {
            ownedDuelsCosmetics = ConcurrentHashMap()
        }

        return ownedDuelsCosmetics!!
    }

    fun hasActiveRankedBan() = rankedBan != null && rankedBan!!.isEffective()
    fun applyRankedBan(duration: Duration)
    {
        val banDuration = if (duration.isPermanent()) null else System.currentTimeMillis() + duration.get()

        rankedBan = RankedBan(
            effectiveUntil = banDuration
        )
    }

    fun deliverRankedBanMessage(player: Player) = rankedBan!!.deliverBanMessage(player)

    fun rankedBanEffectiveUntil() = rankedBan!!.effectiveUntil
    fun removeRankedBan()
    {
        rankedBan = null
    }

    fun save() = DataStoreObjectControllerCache
        .findNotNull<PracticeProfile>()
        .save(this, DataStoreStorageType.MONGO)

}
