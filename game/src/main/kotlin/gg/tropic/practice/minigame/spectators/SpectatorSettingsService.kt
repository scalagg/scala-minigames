package gg.tropic.practice.minigame.spectators

import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.games.GameService
import gg.tropic.practice.profile.PracticeProfileService
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

@Service
object SpectatorSettingsService
{
    private const val LONG_EFFECT_DURATION = 1_000_000
    private const val PREFERENCE_TRACKING = "spectator:tracking"
    private const val PREFERENCE_FIRST_PERSON = "spectator:first-person"
    private const val PREFERENCE_NIGHT_VISION = "spectator:night-vision"

    private val trackedTargets = ConcurrentHashMap<UUID, UUID>()
    private val trackingDisabled = ConcurrentHashMap.newKeySet<UUID>()
    private val firstPersonEnabled = ConcurrentHashMap.newKeySet<UUID>()
    private val nightVisionEnabled = ConcurrentHashMap.newKeySet<UUID>()
    private val loadedPreferences = ConcurrentHashMap.newKeySet<UUID>()
    private val previousGameModes = ConcurrentHashMap<UUID, GameMode>()
    private val spectatorTargetSetter = Player::class.java
        .methods
        .firstOrNull {
            it.name == "setSpectatorTarget" && it.parameterTypes.size == 1
        }

    fun preload(player: Player)
    {
        ensureLoaded(player)
    }

    private fun ensureLoaded(player: Player)
    {
        if (!loadedPreferences.add(player.uniqueId))
        {
            return
        }

        val profile = PracticeProfileService.find(player)
            ?: return

        if (!profile.spectatorPreference(PREFERENCE_TRACKING, true))
        {
            trackingDisabled.add(player.uniqueId)
        }

        if (profile.spectatorPreference(PREFERENCE_FIRST_PERSON, false))
        {
            firstPersonEnabled.add(player.uniqueId)
        }

        if (profile.spectatorPreference(PREFERENCE_NIGHT_VISION, false))
        {
            nightVisionEnabled.add(player.uniqueId)
        }
    }

    private fun persistSetting(player: Player, key: String, value: Boolean)
    {
        val profile = PracticeProfileService.find(player)
            ?: return

        profile.setSpectatorPreference(key, value)
        Schedulers
            .async()
            .run {
                profile.save()
            }
    }

    fun isTrackingEnabled(uniqueId: UUID) = uniqueId !in trackingDisabled
    fun isFirstPersonEnabled(uniqueId: UUID) = uniqueId in firstPersonEnabled
    fun isNightVisionEnabled(uniqueId: UUID) = uniqueId in nightVisionEnabled

    fun firstPersonLocation(spectator: Player, target: Player): Location
    {
        return target.eyeLocation.clone().apply {
            subtract(0.0, spectator.eyeHeight.toDouble(), 0.0)
            yaw = target.location.yaw
            pitch = target.location.pitch
        }
    }

    private fun setSpectatorTarget(spectator: Player, target: Player?): Boolean
    {
        val setter = spectatorTargetSetter
            ?: return false

        return runCatching {
            setter.invoke(spectator, target)
        }
            .isSuccess
    }

    private fun enableFirstPersonTracking(spectator: Player, target: Player)
    {
        if (spectator.gameMode != GameMode.SPECTATOR)
        {
            previousGameModes[spectator.uniqueId] = spectator.gameMode
            spectator.gameMode = GameMode.SPECTATOR
        }

        if (!setSpectatorTarget(spectator, target))
        {
            val location = firstPersonLocation(spectator, target)
            if (shouldSyncTo(spectator, location))
            {
                spectator.teleport(location)
            }
        }
    }

    private fun disableFirstPersonTracking(spectator: Player)
    {
        setSpectatorTarget(spectator, null)

        if (spectator.gameMode == GameMode.SPECTATOR)
        {
            spectator.gameMode = previousGameModes.remove(spectator.uniqueId) ?: GameMode.SURVIVAL
        } else
        {
            previousGameModes.remove(spectator.uniqueId)
        }

        if (GameService.isSpectating(spectator))
        {
            spectator.allowFlight = true
            spectator.isFlying = true
            applySpectatorEffects(spectator)
        }
    }

    fun toggleTracking(player: Player): Boolean
    {
        ensureLoaded(player)

        if (!trackingDisabled.add(player.uniqueId))
        {
            trackingDisabled.remove(player.uniqueId)
        }

        val enabled = isTrackingEnabled(player.uniqueId)
        if (!enabled)
        {
            firstPersonEnabled.remove(player.uniqueId)
            persistSetting(player, PREFERENCE_FIRST_PERSON, false)
            disableFirstPersonTracking(player)
            GameService.audiences.player(player).sendActionBar(Component.empty())
        }

        persistSetting(player, PREFERENCE_TRACKING, enabled)

        return enabled
    }

    fun toggleFirstPerson(player: Player): Boolean
    {
        ensureLoaded(player)

        if (!firstPersonEnabled.add(player.uniqueId))
        {
            firstPersonEnabled.remove(player.uniqueId)
        }

        if (isFirstPersonEnabled(player.uniqueId))
        {
            trackingDisabled.remove(player.uniqueId)
            persistSetting(player, PREFERENCE_TRACKING, true)

            val trackedTarget = trackedTargets[player.uniqueId]
                ?.let(Bukkit::getPlayer)
            if (trackedTarget != null)
            {
                enableFirstPersonTracking(player, trackedTarget)
            }
        } else
        {
            disableFirstPersonTracking(player)
        }

        persistSetting(player, PREFERENCE_FIRST_PERSON, isFirstPersonEnabled(player.uniqueId))

        return isFirstPersonEnabled(player.uniqueId)
    }

    fun toggleNightVision(player: Player): Boolean
    {
        ensureLoaded(player)

        if (!nightVisionEnabled.add(player.uniqueId))
        {
            nightVisionEnabled.remove(player.uniqueId)
        }

        applySpectatorEffects(player)
        persistSetting(player, PREFERENCE_NIGHT_VISION, isNightVisionEnabled(player.uniqueId))
        return isNightVisionEnabled(player.uniqueId)
    }

    fun startTracking(spectator: Player, target: Player, firstPerson: Boolean? = null): Boolean
    {
        ensureLoaded(spectator)

        if (!GameService.isSpectating(spectator))
        {
            return false
        }

        if (target.uniqueId == spectator.uniqueId || GameService.isSpectating(target))
        {
            clearTrackedTarget(spectator.uniqueId)
            return false
        }

        trackedTargets[spectator.uniqueId] = target.uniqueId
        trackingDisabled.remove(spectator.uniqueId)
        persistSetting(spectator, PREFERENCE_TRACKING, true)

        if (firstPerson != null)
        {
            if (firstPerson)
            {
                firstPersonEnabled.add(spectator.uniqueId)
            } else
            {
                firstPersonEnabled.remove(spectator.uniqueId)
            }

            persistSetting(spectator, PREFERENCE_FIRST_PERSON, firstPerson)
        }

        if (isFirstPersonEnabled(spectator.uniqueId))
        {
            enableFirstPersonTracking(spectator, target)
        } else
        {
            disableFirstPersonTracking(spectator)
        }

        return true
    }

    fun setTrackedTarget(spectator: Player, target: Player)
    {
        startTracking(spectator, target)
    }

    fun clearTrackedTarget(spectatorUniqueId: UUID, clearActionBar: Boolean = true)
    {
        trackedTargets.remove(spectatorUniqueId)

        val spectator = Bukkit.getPlayer(spectatorUniqueId)
        if (spectator != null)
        {
            disableFirstPersonTracking(spectator)
        }

        if (!clearActionBar)
        {
            return
        }

        if (spectator != null)
        {
            GameService.audiences.player(spectator).sendActionBar(Component.empty())
        }
    }

    private fun shouldSyncTo(spectator: Player, destination: Location): Boolean
    {
        val spectatorLocation = spectator.location
        if (spectatorLocation.world != destination.world)
        {
            return true
        }

        if (spectatorLocation.distanceSquared(destination) > 0.0625)
        {
            return true
        }

        return abs(spectatorLocation.yaw - destination.yaw) > 2f ||
            abs(spectatorLocation.pitch - destination.pitch) > 2f
    }

    fun applySpectatorEffects(player: Player)
    {
        ensureLoaded(player)

        if (!GameService.isSpectating(player))
        {
            player.removePotionEffect(PotionEffectType.INVISIBILITY)
            player.removePotionEffect(PotionEffectType.NIGHT_VISION)
            return
        }

        player.addPotionEffect(
            PotionEffect(PotionEffectType.INVISIBILITY, LONG_EFFECT_DURATION, 0, true, false),
            true
        )

        if (isNightVisionEnabled(player.uniqueId))
        {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.NIGHT_VISION, LONG_EFFECT_DURATION, 0, true, false),
                true
            )
        } else
        {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION)
        }
    }

    private fun clearState(uniqueId: UUID)
    {
        trackedTargets.remove(uniqueId)
        trackingDisabled.remove(uniqueId)
        firstPersonEnabled.remove(uniqueId)
        nightVisionEnabled.remove(uniqueId)
        loadedPreferences.remove(uniqueId)
        previousGameModes.remove(uniqueId)
    }

    @Configure
    fun configure()
    {
        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                clearState(it.player.uniqueId)
            }

        Events
            .subscribe(PlayerInteractEntityEvent::class.java)
            .filter {
                it.rightClicked is Player && GameService.isSpectating(it.player)
            }
            .handler {
                it.isCancelled = true

                val target = it.rightClicked as Player
                if (!startTracking(it.player, target, firstPerson = true))
                {
                    return@handler
                }

                it.player.teleport(firstPersonLocation(it.player, target))
                it.player.sendMessage("${CC.GREEN}Now spectating ${CC.WHITE}${target.name}${CC.GREEN} in first person.")
            }

        Events
            .subscribe(PlayerToggleSneakEvent::class.java)
            .filter {
                it.isSneaking &&
                    GameService.isSpectating(it.player) &&
                    isFirstPersonEnabled(it.player.uniqueId) &&
                    trackedTargets.containsKey(it.player.uniqueId)
            }
            .handler {
                firstPersonEnabled.remove(it.player.uniqueId)
                persistSetting(it.player, PREFERENCE_FIRST_PERSON, false)

                disableFirstPersonTracking(it.player)
                it.player.sendMessage("${CC.YELLOW}Stopped first person spectating. ${CC.GRAY}You are still tracking your target.")
            }

        Schedulers
            .sync()
            .runRepeating(Runnable {
                val toClear = mutableListOf<UUID>()

                trackedTargets.forEach { (spectatorId, targetId) ->
                    val spectator = Bukkit.getPlayer(spectatorId)
                    if (spectator == null || !GameService.isSpectating(spectator))
                    {
                        if (spectator != null)
                        {
                            GameService.audiences.player(spectator).sendActionBar(Component.empty())
                        }

                        toClear += spectatorId
                        return@forEach
                    }

                    ensureLoaded(spectator)

                    if (!isTrackingEnabled(spectatorId))
                    {
                        disableFirstPersonTracking(spectator)
                        GameService.audiences.player(spectator).sendActionBar(Component.empty())
                        return@forEach
                    }

                    val target = Bukkit.getPlayer(targetId)
                    val spectatorGame = GameService.byPlayerOrSpectator(spectator.uniqueId)
                    val targetGame = target?.let { GameService.byPlayerOrSpectator(it.uniqueId) }

                    if (target == null || targetGame == null || spectatorGame == null)
                    {
                        toClear += spectatorId
                        GameService.audiences.player(spectator).sendActionBar(Component.empty())
                        return@forEach
                    }

                    if (
                        spectatorGame.expectation != targetGame.expectation ||
                        GameService.isSpectating(target)
                    )
                    {
                        toClear += spectatorId
                        GameService.audiences.player(spectator).sendActionBar(Component.empty())
                        return@forEach
                    }

                    if (spectator.world != target.world)
                    {
                        return@forEach
                    }

                    if (isFirstPersonEnabled(spectatorId))
                    {
                        enableFirstPersonTracking(spectator, target)
                    } else
                    {
                        disableFirstPersonTracking(spectator)
                    }

                    val distance = spectator.location.distance(target.location)
                    val actionBar = "${CC.WHITE}Target: ${CC.GREEN}${target.name}${CC.WHITE} Distance: ${CC.GREEN}${String.format(Locale.US, "%.1f", distance)}m"
                    GameService.audiences.player(spectator).sendActionBar(
                        LegacyComponentSerializer.legacySection().deserialize(actionBar)
                    )
                }

                toClear.forEach { trackedTargets.remove(it) }
            }, 0L, 2L)
    }
}
