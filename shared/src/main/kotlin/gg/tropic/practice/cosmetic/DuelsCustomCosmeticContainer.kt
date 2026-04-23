package gg.tropic.practice.cosmetic

data class DuelsCustomCosmeticContainer(
    val trims: MutableList<DuelsArmorTrimCosmeticDefinition> = mutableListOf(),
    val shieldBanners: MutableList<DuelsShieldBannerCosmeticDefinition> = mutableListOf()
)
{
    fun trimById(id: String) = trims.firstOrNull { it.id.equals(id, true) }
    fun shieldBannerById(id: String) = shieldBanners.firstOrNull { it.id.equals(id, true) }
}
