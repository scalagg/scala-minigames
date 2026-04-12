package mc.arch.minigames.versioned.legacy

import mc.arch.minigames.versioned.generics.VersionedProviders

object LegacyVersionedProviders : VersionedProviders
{
    override fun getSlimeProvider() = LegacySlimeProvider
    override fun getPotionProvider() = LegacyPotionProvider
    override fun getPlayerProvider() = LegacyPlayerProvider
}
