package gg.tropic.practice.extensions

import org.bukkit.inventory.ItemStack

fun Array<ItemStack?>.deepClone(): Array<ItemStack?>
{
    val copy = arrayOfNulls<ItemStack?>(size = size)
    forEachIndexed { index, itemStack ->
        copy[index] = itemStack?.clone()
    }

    return copy
}
