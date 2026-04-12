package gg.tropic.practice.games

import dev.iiahmed.disguise.Disguise

import gg.scala.basics.plugin.disguise.DisguiseService
import gg.tropic.practice.expectation.ExpectationService.returnToSpawnItem
import gg.tropic.practice.expectation.ExpectationService.spectatorSettingsItem
import gg.tropic.practice.expectation.ExpectationService.spectateItem
import gg.tropic.practice.games.GameService.plugin
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.extensions.resetAttributes
import gg.tropic.practice.minigame.spectators.SpectatorSettingsService
import gg.tropic.practice.strategies.MarkSpectatorStrategy.playAgainItem
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.visibility.VisibilityHandler
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scoreboard.DisplaySlot

private fun GameImpl.syncSpectatorVisibilityFor(player: Player)
{
    val playerIsSpectator = GameService.isSpectating(player)

    this.toBukkitPlayers()
        .filterNotNull()
        .filter { it.uniqueId != player.uniqueId }
        .forEach { other ->
            val otherIsSpectator = GameService.isSpectating(other)

            if (playerIsSpectator && !otherIsSpectator)
            {
                other.hidePlayer(player)
            } else
            {
                other.showPlayer(player)
            }

            if (otherIsSpectator && !playerIsSpectator)
            {
                player.hidePlayer(other)
            } else
            {
                player.showPlayer(other)
            }
        }
}

fun GameImpl.resetPlayerForFight(player: Player, noUpdateVisibility: Boolean = false)
{
    if (!noUpdateVisibility)
    {
        VisibilityHandler.update(player)
        NametagHandler.reloadPlayer(player)

        this.toBukkitPlayers()
            .filterNotNull()
            .forEach { other ->
                VisibilityHandler.update(other)
                NametagHandler.reloadPlayer(player, other)
            }

        syncSpectatorVisibilityFor(player)
    }

    player.apply {
        isFlying = false
        allowFlight = false
        resetAttributes()
    }

    if (flag(FeatureFlag.EntityDisguise))
    {
        val type = flagMetaData(FeatureFlag.EntityDisguise, "type")
            ?: "PLAYER"

        DisguiseService.provider().disguise(
            player,
            Disguise
                .builder()

                .build()
        )
    }
}

fun GameImpl.enterSpectatorAfterLifeEnd(player: Player, final: Boolean = false)
{
    player.apply {
        GameService.spectatorPlayers += player.uniqueId

        VisibilityHandler.update(player)
        NametagHandler.reloadPlayer(player)

        resetAttributes(editFlightAttributes = false)

        player.allowFlight = true
        player.isFlying = true
        SpectatorSettingsService.clearTrackedTarget(player.uniqueId)
        SpectatorSettingsService.applySpectatorEffects(player)

        if (final)
        {
            player.inventory.setItem(0, spectateItem)
            player.inventory.setItem(4, spectatorSettingsItem)
            player.inventory.setItem(7, playAgainItem)
            player.inventory.setItem(8, returnToSpawnItem)
            player.updateInventory()
        }

        if (flag(FeatureFlag.EntityDisguise))
        {
            DisguiseService.provider().undisguise(player)
        }
    }

    this.toBukkitPlayers()
        .filterNotNull()
        .forEach { other ->
            VisibilityHandler.update(other)
            NametagHandler.reloadPlayer(player, other)
        }

    syncSpectatorVisibilityFor(player)
}

fun GameImpl.prepareForNewLife(player: Player, volatile: Boolean, noUpdateVisibility: Boolean = false): Boolean
{
    if (!teleportToSpawnLocation(player))
    {
        return false
    }

    if (!noUpdateVisibility)
    {
        GameService.spectatorPlayers -= player.uniqueId
    }

    player.getMetadata("life").firstOrNull()
        ?.value()
        ?.apply {
            val terminable = this as CompositeTerminable
            terminable.closeAndReportException()

            player.setMetadata(
                "life",
                FixedMetadataValue(plugin, CompositeTerminable.create())
            )
        }

    resetPlayerForFight(player, noUpdateVisibility)

    if (volatile)
    {
        Schedulers
            .sync()
            .runLater({
                if (player.isOnline)
                {
                    applyPreSelectedLoadouts(player)
                }
            }, 3L)
    } else
    {
        applyPreSelectedLoadouts(player)
    }

    return true
}

fun GameImpl.teleportToSpawnLocation(player: Player): Boolean
{
    val team = getNullableTeam(player)
        ?: return false

    val spawnLocation = map
        .findSpawnLocationMatchingTeam(
            team.teamIdentifier
        )
        ?.toLocation(arenaWorld)
        ?: return false

    if (flag(FeatureFlag.ClearIllegalBlocksOnRespawn))
    {
        val block = arenaWorld.getBlockAt(spawnLocation)
        block.type = Material.AIR

        block.getRelative(BlockFace.UP).apply {
            type = Material.AIR
        }
    }

    player.teleport(spawnLocation)
    return true
}

fun GameImpl.applyPreSelectedLoadouts(player: Player)
{
    loadout(player).apply(player)
}
