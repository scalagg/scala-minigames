package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Single
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.category.KitCategory
import gg.tropic.practice.kit.category.KitCategoryService
import net.evilblock.cubed.menu.menus.TextEditorMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.Material
import org.bukkit.entity.Player

@AutoRegister
@CommandPermission("practice.command.kitcategory")
@CommandAlias("kitcategory|kitcategories|kc")
object KitCategoryCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("list")
    @Description("List all categories.")
    fun onList(player: ScalaPlayer)
    {
        val categories = KitCategoryService.cached().categories

        player.sendMessage(
            "${CC.GREEN}All kit categories:",
            "${CC.WHITE}[click a kit category to view information]"
        )

        val listFancyMessage = FancyMessage()
        for ((index, category) in categories.withIndex())
        {
            val infoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${category.id}${CC.WHITE}${if (index != categories.size - 1) ", " else "."}"
                )
                .andHoverOf("${CC.GRAY}Click to view kit category information.")
                .andCommandOf(ClickEvent.Action.RUN_COMMAND, "/kitcategory info ${category.id}")

            listFancyMessage.components.addAll(infoComponent.components)
        }

        if (listFancyMessage.components.isNotEmpty())
        {
            listFancyMessage.sendToPlayer(player.bukkit())
        }
        else
        {
            player.sendMessage("${CC.RED}None")
        }
    }

    @Subcommand("info")
    @CommandCompletion("@categories")
    @Description("Show a category.")
    fun onInfo(player: ScalaPlayer, category: KitCategory)
    {
        player.sendMessage(
            "${CC.PRI}${category.id}",
            "${CC.GRAY}Name: ${CC.WHITE}${category.name}",
            "${CC.GRAY}New: ${CC.WHITE}${category.newCategory}",
            "${CC.GRAY}Item: ${CC.WHITE}${category.item.name}",
            "",
            "${CC.PRI}Description:",
            *(if (category.description.isEmpty()) listOf("${CC.RED}None") else category.description).toTypedArray(),
            "",
            "${CC.PRI}Kit bindings:",
            *KitService.cached().kits.values
                .filter { it.id in category.kits }
                .map { " - ${it.displayName}" }
                .toTypedArray()
        )
    }

    @AssignPermission
    @Subcommand("displayicon")
    @CommandCompletion("@categories")
    @Description("Set a category's material.")
    fun onDisplayIcon(player: ScalaPlayer, category: KitCategory, material: Material)
    {
        category.item = material
        KitCategoryService.sync(KitCategoryService.cached())

        player.sendMessage(
            "${CC.GREEN}You set the material for ${CC.YELLOW}${category.id}${CC.GREEN} to ${CC.WHITE}${material}${CC.GREEN}!"
        )
    }

    @AssignPermission
    @Subcommand("displayname")
    @CommandCompletion("@categories")
    @Description("Set a category's display name.")
    fun onDisplayName(player: ScalaPlayer, category: KitCategory, name: String)
    {
        category.name = name
        KitCategoryService.sync(KitCategoryService.cached())

        player.sendMessage(
            "${CC.GREEN}You set the name for ${CC.YELLOW}${category.id}${CC.GREEN} to ${CC.WHITE}${category.name}${CC.GREEN}!"
        )
    }

    @AssignPermission
    @Subcommand("description")
    @CommandCompletion("@categories")
    @Description("Edit a category's description.")
    fun onDescription(player: ScalaPlayer, category: KitCategory)
    {
        object : TextEditorMenu(category.description)
        {
            override fun getPrePaginatedTitle(player: Player) = "Edit description for: ${category.id}"

            override fun onClose(player: Player)
            {
            }

            override fun onSave(player: Player, list: List<String>)
            {
                category.description = list.toMutableList()
                KitCategoryService.sync(KitCategoryService.cached())

                player.sendMessage(
                    "${CC.GREEN}You updated the description for ${CC.YELLOW}${category.id}${CC.GREEN}!"
                )
            }
        }.openMenu(player.bukkit())
    }

    @AssignPermission
    @Subcommand("newcategory")
    @CommandCompletion("@categories")
    @Description("Toggle whether a category is marked as new.")
    fun onNewCategory(player: ScalaPlayer, category: KitCategory)
    {
        category.newCategory = !category.newCategory
        KitCategoryService.sync(KitCategoryService.cached())

        player.sendMessage(
            "${CC.GREEN}You ${if (category.newCategory) "${CC.D_GREEN}enabled" else "${CC.RED}disabled"}${CC.GREEN} new mode for ${CC.YELLOW}${category.id}${CC.GREEN}!"
        )
    }

    @AssignPermission
    @Subcommand("delete")
    @CommandCompletion("@categories")
    @Description("Delete a kit category.")
    fun onDelete(player: ScalaPlayer, category: KitCategory)
    {
        val model = KitCategoryService.cached()
        model.categories -= category
        KitCategoryService.sync(model)

        player.sendMessage(
            "${CC.RED}You removed the kit category ${CC.D_RED}${category.id}${CC.RED}!"
        )
    }

    @AssignPermission
    @Subcommand("create")
    @Description("Create a new kit category.")
    fun onCreate(player: ScalaPlayer, @Single name: String)
    {
        if (KitCategoryService.cached().findById(name) != null)
        {
            throw ConditionFailedException("A kit category by that name already exists!")
        }

        val model = KitCategoryService.cached()
        model.categories += KitCategory(name)
        KitCategoryService.sync(model)

        player.sendMessage(
            "${CC.GREEN}You created a new kit category with the name ${CC.D_GREEN}$name${CC.GREEN}!"
        )
    }
}
