package gg.tropic.practice.versioned

import com.cryptomorin.xseries.XSound
import net.evilblock.cubed.util.ServerVersion
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.Sound
import org.bukkit.entity.Player
import kotlin.math.roundToInt

object MinigameModernAccess
{
    fun isModern() = ServerVersion.getVersion().isNewerThan(ServerVersion.v1_20_R3)

    private fun material(name: String, fallback: Material): Material = kotlin.runCatching {
        java.lang.Enum.valueOf(Material::class.java, name)
    }.getOrNull() ?: Material.matchMaterial(name) ?: fallback

    private fun material(name: String, fallbackName: String, hardFallback: Material): Material
    {
        return kotlin.runCatching { java.lang.Enum.valueOf(Material::class.java, name) }.getOrNull()
            ?: Material.matchMaterial(name)
            ?: kotlin.runCatching { java.lang.Enum.valueOf(Material::class.java, fallbackName) }.getOrNull()
            ?: Material.matchMaterial(fallbackName)
            ?: hardFallback
    }

    fun lobbyNavigatorMaterial(): Material = if (isModern()) material("RECOVERY_COMPASS", Material.COMPASS) else Material.COMPASS
    fun cosmeticsMaterial(): Material = if (isModern()) material("ENDER_CHEST", Material.CHEST) else Material.CHEST
    fun profileHeadMaterial(): Material = if (isModern()) material("PLAYER_HEAD", "SKULL_ITEM", Material.COMPASS) else material("SKULL_ITEM", "PLAYER_HEAD", Material.COMPASS)
    fun menuAccentMaterial(): Material = if (isModern()) material("ECHO_SHARD", Material.NETHER_STAR) else Material.NETHER_STAR
    fun leaderboardMaterial(): Material = if (isModern()) material("TRIAL_KEY", "ITEM_FRAME", Material.ITEM_FRAME) else material("ITEM_FRAME", "PAINTING", Material.PAINTING)
    fun settingsMaterial(): Material = if (isModern()) material("OMINOUS_BOTTLE", "COMPARATOR", Material.COMPASS) else material("REDSTONE_COMPARATOR", "COMPARATOR", Material.COMPASS)
    fun casualQueueMaterial(): Material = if (isModern()) material("WIND_CHARGE", Material.IRON_SWORD) else Material.IRON_SWORD
    fun rankedQueueMaterial(): Material = if (isModern()) material("NETHERITE_SWORD", Material.DIAMOND_SWORD) else Material.DIAMOND_SWORD
    fun botQueueMaterial(): Material = if (isModern()) material("MACE", "GOLDEN_SWORD", Material.IRON_SWORD) else material("GOLD_SWORD", "GOLDEN_SWORD", Material.IRON_SWORD)
    fun loadoutMaterial(): Material = if (isModern()) material("CHISELED_BOOKSHELF", Material.BOOK) else Material.BOOK

    fun duelsCasualQueueMaterial(): Material = Material.IRON_SWORD
    fun duelsCompetitiveQueueMaterial(): Material = Material.DIAMOND_SWORD
    fun duelsPracticeQueueMaterial(): Material = if (isModern()) material("MACE", "GOLDEN_SWORD", Material.IRON_SWORD) else material("GOLD_SWORD", "GOLDEN_SWORD", Material.IRON_SWORD)
    fun duelsCreatePartyMaterial(): Material = if (isModern()) material("GOAT_HORN", "LEAD", Material.COMPASS) else material("LEASH", "LEAD", Material.COMPASS)
    fun duelsCosmeticsMaterial(): Material = material("ARMOR_STAND", Material.CHEST)
    fun duelsNavigatorMaterial(): Material = if (isModern()) material("BELL", Material.COMPASS) else Material.COMPASS
    fun duelsSettingsMaterial(): Material = if (isModern()) material("LECTERN", "COMPARATOR", Material.COMPASS) else material("REDSTONE_COMPARATOR", "COMPARATOR", Material.COMPASS)

    fun applySingGoatHorn(itemStack: ItemStack): ItemStack
    {
        if (!isModern() || itemStack.type != duelsCreatePartyMaterial())
        {
            return itemStack
        }

        val meta = itemStack.itemMeta
            ?: return itemStack

        kotlin.runCatching {
            val instrumentClass = Class.forName("org.bukkit.MusicInstrument")
            val setInstrument = meta.javaClass.getMethod("setInstrument", instrumentClass)

            val instrument = kotlin.runCatching {
                instrumentClass.getField("SING_GOAT_HORN").get(null)
            }.getOrElse {
                val registryClass = Class.forName("org.bukkit.Registry")
                val namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey")
                val instrumentRegistry = registryClass.getField("INSTRUMENT").get(null)
                val key = namespacedKeyClass.getMethod("minecraft", String::class.java)
                    .invoke(null, "sing_goat_horn")
                instrumentRegistry.javaClass.getMethod("get", namespacedKeyClass)
                    .invoke(instrumentRegistry, key)
            } ?: return itemStack

            setInstrument.invoke(meta, instrument)
            itemStack.itemMeta = meta
        }

        return itemStack
    }

    fun playCreatePartySound(player: Player)
    {
        val sound = if (isModern()) {
            XSound.UI_BUTTON_CLICK.parseSound()
                ?: XSound.BLOCK_NOTE_BLOCK_HAT.parseSound()
        } else {
            XSound.UI_BUTTON_CLICK.parseSound()
        } ?: return

        player.playSound(player.location, sound, 1.0f, if (isModern()) 1.15f else 1.0f)
    }

    fun gradient(text: String, fromHex: String, toHex: String): String
    {
        if (!isModern())
        {
            return text
        }

        val from = parseHexColor(fromHex)
        val to = parseHexColor(toHex)
        val builder = StringBuilder()
        val lastIndex = (text.length - 1).coerceAtLeast(1)

        text.forEachIndexed { index, character ->
            if (character == ' ')
            {
                builder.append(character)
                return@forEachIndexed
            }

            val progress = index.toDouble() / lastIndex.toDouble()
            val red = lerp(from.first, to.first, progress)
            val green = lerp(from.second, to.second, progress)
            val blue = lerp(from.third, to.third, progress)

            appendHexColor(builder, red, green, blue)
            builder.append(character)
        }

        return builder.toString()
    }

    private fun parseHexColor(hex: String): Triple<Int, Int, Int>
    {
        val normalized = hex.removePrefix("#")
        return Triple(
            normalized.substring(0, 2).toInt(16),
            normalized.substring(2, 4).toInt(16),
            normalized.substring(4, 6).toInt(16)
        )
    }

    private fun lerp(start: Int, end: Int, progress: Double): Int
    {
        return (start + ((end - start) * progress)).roundToInt().coerceIn(0, 255)
    }

    private fun appendHexColor(builder: StringBuilder, red: Int, green: Int, blue: Int)
    {
        val color = "%02x%02x%02x".format(red, green, blue)
        builder.append(org.bukkit.ChatColor.COLOR_CHAR).append('x')
        color.forEach {
            builder.append(org.bukkit.ChatColor.COLOR_CHAR).append(it)
        }
    }

    fun playMenuSound(player: Player)
    {
        val sound = if (isModern()) {
            XSound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT.parseSound() ?: XSound.UI_BUTTON_CLICK.parseSound()
        } else {
            XSound.UI_BUTTON_CLICK.parseSound()
        } ?: return

        player.playSound(player.location, sound, 20.0f, if (isModern()) 1.15f else 1.0f)
    }

    fun playQueueJoinSound(player: Player)
    {
        val sound = if (isModern()) {
            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.parseSound() ?: XSound.UI_BUTTON_CLICK.parseSound()
        } else {
            XSound.UI_BUTTON_CLICK.parseSound()
        } ?: return

        player.playSound(player.location, sound, 1.0f, if (isModern()) 1.2f else 1.0f)
    }

    fun playQueueLeaveSound(player: Player)
    {
        val sound = if (isModern()) {
            XSound.BLOCK_NOTE_BLOCK_BASS.parseSound() ?: XSound.UI_BUTTON_CLICK.parseSound()
        } else {
            XSound.BLOCK_NOTE_BLOCK_BASS.parseSound()
        } ?: return

        player.playSound(player.location, sound, 1.0f, if (isModern()) 0.8f else 1.0f)
    }

    fun modernScoreAccent(): String = if (isModern()) "&b◆" else "&7┃"
    fun modernScoreLabel(): String = if (isModern()) "&f" else "&f"
}
