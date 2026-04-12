package gg.tropic.practice.menu

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.queue.QueueType
import me.lucko.helper.Schedulers
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import com.cryptomorin.xseries.XMaterial
import kotlin.text.get

/**
 * @date 1/18/2025
 */
class FavoriteKitsMenu(
    player: Player,
    private val queueType: QueueType,
    private val teamSize: Int
) : TemplateKitMenu(player) {

    init {
        updateAfterClick = true
    }

    override fun getPrePaginatedTitle(player: Player) = "Favorite Kits"

    override fun filterDisplayOfKit(player: Player, kit: Kit) = kit.queueSizes
        .any {
            it.first == teamSize && queueType in it.second
        }

    override fun shouldIncludeKitDescription() = false

    override fun itemTitleFor(player: Player, kit: Kit): String {
        val profile = PracticeProfileService.find(player)!!
        return if (profile.favoriteKits.contains(kit.id)) {
            "${CC.GREEN}${kit.displayName}"
        } else {
            "${CC.RED}${kit.displayName}"
        }
    }

    override fun itemDescriptionOf(player: Player, kit: Kit): List<String> {
        val profile = PracticeProfileService.find(player)!!
        return listOf(
            "",
            if (profile.favoriteKits.contains(kit.id)) {
                "${CC.YELLOW}Click to disable ${kit.displayName}!"
            } else {
                "${CC.YELLOW}Click to enable ${kit.displayName}!"
            }
        )
    }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType) {
        val profile = PracticeProfileService.find(player)!!

        if (profile.favoriteKits.contains(kit.id)) {
            profile.favoriteKits.remove(kit.id)
        } else {
            if (profile.favoriteKits.size >= 5) {
                player.sendMessage("${CC.RED}You can only have up to 5 favorite kits.")
                return
            }
            profile.favoriteKits.add(kit.id)
        }
    }

    override fun getGlobalButtons(player: Player): Map<Int, Button> {
        val profile = PracticeProfileService.find(player)!!
        val buttons = mutableMapOf<Int, Button>()

        buttons[2] = ItemBuilder
            .of(XMaterial.LIME_DYE)
            .name("${CC.GREEN}Save")
            .addToLore(
                "${CC.GRAY}Save your selected kits you've selected",
                "${CC.GRAY}as your favorite kits.",
                "",
                "${CC.GREEN}Click here to save."
            )
            .toButton { _, _ ->
                Schedulers.async().run {
                    profile.save()
                    JoinQueueMenu(player, queueType, 1).openMenu(player)
                }
            }

        buttons[4] = ItemBuilder
            .of(Material.NETHER_STAR)
            .name("${CC.YELLOW}Favorite Kits")
            .addToLore(
                "${CC.GRAY}Your favorite kits,",
                "${CC.GRAY}that appear in the favorited section",
                "${CC.GRAY}of the queue menu.",
                "",
                "${CC.GOLD}Favorite Kits:"
            )
            .apply {
                if (profile.favoriteKits.isEmpty()) {
                    addToLore("${CC.GRAY} - ${CC.RED}None")
                } else {
                    profile.favoriteKits.forEach { kitId ->
                        val kit = KitService.cached().kits[kitId]
                        addToLore("${CC.GRAY} - ${CC.YELLOW}${kit?.displayName ?: "Unknown Kit"}")
                    }
                }
            }
            .addToLore(
                "",
                "${CC.GREEN}Click on the kits",
                "${CC.GREEN}you wish to add/remove."
            )
            .toButton()

        buttons[6] = ItemBuilder
            .of(XMaterial.RED_DYE)
            .name("${CC.RED}Cancel")
            .addToLore(
                "${CC.GRAY}Cancel updating ",
                "${CC.GRAY}your Favorite Kits.",
                "",
                "${CC.GREEN}Click here to cancel"
            )
            .toButton { _, _ ->
                JoinQueueMenu(player, queueType, 1).openMenu(player)
            }

        return buttons
    }
}
