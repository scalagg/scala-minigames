package gg.tropic.practice.extensions

import gg.tropic.practice.versioned.Versioned
import org.bukkit.Location
import org.bukkit.entity.Player

fun Player.mountImmovable() = Versioned.toProvider()
    .getPlayerProvider()
    .mountImmovable(this, location)

fun Player.mountImmovable(location: Location) = Versioned.toProvider()
    .getPlayerProvider()
    .mountImmovable(this, location)

fun Player.unmount() = Versioned.toProvider()
    .getPlayerProvider()
    .unmount(this)

fun Player.addOrDrop(vararg itemStack: org.bukkit.inventory.ItemStack)
{
    val added = inventory.addItem(*itemStack)
    added.forEach {
        world.dropItemNaturally(location, it.value)
    }
}
