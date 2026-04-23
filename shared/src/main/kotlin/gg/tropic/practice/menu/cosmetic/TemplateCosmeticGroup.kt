package gg.tropic.practice.menu.cosmetic

import org.bukkit.Material

/**
 * @author Limo
 * @date 2/21/2026
 */
interface TemplateCosmeticGroup
{
    val groupId: String
    val displayName: String
    val description: List<String>
    val iconMaterial: Material
    val iconData: Short
    val menuSlot: Int
    val previewable: Boolean
    val skullTexture: String?

    fun entries(): List<TemplateCosmeticItem>
    fun selectedId(player: org.bukkit.entity.Player): String
    fun select(player: org.bukkit.entity.Player, id: String)
    fun ownedIds(player: org.bukkit.entity.Player): Set<String>
    fun ownedCount(player: org.bukkit.entity.Player): Int
    fun unlock(player: org.bukkit.entity.Player, id: String): Boolean
    fun saveProfile(player: org.bukkit.entity.Player)
}
