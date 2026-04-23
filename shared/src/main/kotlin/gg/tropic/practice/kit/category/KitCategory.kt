package gg.tropic.practice.kit.category

import gg.tropic.practice.kit.KitService
import org.bukkit.Material

data class KitCategory(
    val id: String,
    val kits: MutableList<String> = mutableListOf(),
    var name: String = id.replace('-', ' ').replaceFirstChar { it.uppercase() },
    var item: Material = Material.PAPER,
    var newCategory: Boolean = true,
    var description: MutableList<String> = mutableListOf()
)
{
    fun kits() = kits.mapNotNull {
        KitService.cached().kits[it]
    }
}
