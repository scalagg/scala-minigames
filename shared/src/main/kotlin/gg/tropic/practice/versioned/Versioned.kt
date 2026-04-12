package gg.tropic.practice.versioned

import mc.arch.minigames.versioned.legacy.LegacyVersionedProviders
import mc.arch.minigames.versioned.modern.ModernVersionedProviders
import net.evilblock.cubed.util.ServerVersion

object Versioned
{
    private val provider by lazy {
        if (ServerVersion.getVersion().isOlderThan(ServerVersion.v1_9))
        {
            LegacyVersionedProviders
        } else
        {
            ModernVersionedProviders
        }
    }

    fun toProvider() = provider
}
