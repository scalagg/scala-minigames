package mc.arch.minigames.versioned.legacy

import mc.arch.minigames.versioned.generics.PlayerProvider
import org.bukkit.Location
import org.bukkit.entity.Player

object LegacyPlayerProvider : PlayerProvider
{
    override fun obfuscateHealth(player: Player, obfuscated: Boolean)
    {

    }

    // Original packet-based fake-bat impl was lost during 1.21 migration
    // (its imports already pointed at modern Paper paths and didn't compile).
    // Restore from git history if legacy 1.8 still needs the freeze-in-place behavior.
    override fun mountImmovable(player: Player, location: Location)
    {

    }

    override fun unmount(player: Player)
    {

    }

    // Pre-1.19 dataWatcher.watch<Byte>(9, -1) hack — was inside an isOlderThan(v1_19) gate
    // in shared/EntityExtensions. Restore from git history if a 1.8 build still needs it.
    override fun applyResetAttributesLegacyHack(player: Player)
    {

    }
}
