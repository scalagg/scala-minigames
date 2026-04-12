package mc.arch.minigames.versioned.generics

interface VersionedProviders
{
    fun getSlimeProvider(): SlimeProvider
    fun getPotionProvider(): PotionProvider
    fun getPlayerProvider(): PlayerProvider
}
