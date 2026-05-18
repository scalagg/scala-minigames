package gg.tropic.practice.minigame.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.commons.acf.ConditionFailedException
import gg.tropic.practice.commands.RejoinCommand
import gg.tropic.practice.minigame.MiniGameModeMetadata
import gg.tropic.practice.minigame.joinGame
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

data class MinigameNPCPlayMenu(
    private val modeMetadata: MiniGameModeMetadata
) : Menu("Play ${modeMetadata.displayName}")
{
    override fun size(buttons: Map<Int, Button>) = 27
    fun Player.joinItem() = modeMetadata.toItem()
        .addToLore(
            "",
            "${CC.YELLOW}Click to play!"
        )
        .toButton { _, _ ->
            Button.playNeutral(this)
            this.closeInventory()
            modeMetadata.joinGame(this)
        }

    fun Player.selectMapItem() = ItemBuilder
        .of(XMaterial.OAK_SIGN)
        .name("${CC.GREEN}Map Selection")
        .addToLore(
            "${CC.GRAY}Select a specific map",
            "${CC.GRAY}to play on!",
            "",
            "${CC.YELLOW}Click to select!"
        )
        .toButton { _, _ ->
            Button.playNeutral(this)
            MinigameMapSelectorMenu({
                MinigameNPCPlayMenu(modeMetadata).openMenu(this)
            }, modeMetadata).openMenu(this)
        }

    fun Player.rejoinItem() = ItemBuilder
        .of(XMaterial.ENDER_PEARL)
        .name("${CC.GREEN}Rejoin Game")
        .addToLore(
            "${CC.GRAY}Rejoin the game you",
            "${CC.GRAY}were previously in!",
            "",
            "${CC.YELLOW}Click to select!"
        )
        .toButton { _, _ ->
            Button.playNeutral(this)
            this.closeInventory()
            RejoinCommand.onRejoin(this, null)
                .exceptionally { throwable ->
                    if (throwable is ConditionFailedException)
                    {
                        this.sendMessage("${CC.RED}${throwable.message}")
                    }
                    return@exceptionally null
                }
        }

    override fun getButtons(player: Player) = when (true)
    {
        (modeMetadata.allowMapSelection && modeMetadata.allowRejoins) -> mapOf(
            11 to player.joinItem(),
            13 to player.selectMapItem(),
            15 to player.rejoinItem()
        )
        (modeMetadata.allowMapSelection && !modeMetadata.allowRejoins) -> mapOf(
            12 to player.joinItem(),
            14 to player.selectMapItem()
        )
        (!modeMetadata.allowMapSelection && !modeMetadata.allowRejoins) -> mapOf(
            13 to player.joinItem()
        )
        else -> mapOf(
            11 to player.joinItem(),
            13 to player.selectMapItem(),
            15 to player.rejoinItem()
        )
    }
}
