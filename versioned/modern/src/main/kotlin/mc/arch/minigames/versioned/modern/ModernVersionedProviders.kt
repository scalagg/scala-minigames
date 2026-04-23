package mc.arch.minigames.versioned.modern

import mc.arch.minigames.versioned.generics.PlayerProvider
import mc.arch.minigames.versioned.generics.PotionProvider
import mc.arch.minigames.versioned.generics.VersionedProviders

object ModernVersionedProviders : VersionedProviders
{
    override fun getSlimeProvider() = ModernSlimeProvider
    override fun getPotionProvider() = ModernPotionProvider
    override fun getPlayerProvider() = ModernPlayerProvider
}
