package mc.arch.minigames.versioned.generics

import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionType

interface PotionProvider
{
    fun buildPotion(
        type: PotionType,
        name: String,
        splash: Boolean,
        amount: Int,
        level: Int,
        duration: Int,
        customEffects: List<PotionEffect>
    ): ItemStack
}
