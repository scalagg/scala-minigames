package gg.solara.practice.editor

import org.bukkit.Bukkit
import java.io.File

class Containers
{
    val pluginsDirectory = File(
        Bukkit.getWorldContainer(),
        "plugins"
    )

    val worldEditDirectory = File(
        pluginsDirectory,
        "WorldEdit"
    )

    val devToolsWorldsDirectory = File(
        File(
            pluginsDirectory,
            "DevTools"
        ),
        "worlds"
    )

    val schematicsDirectory = File(
        worldEditDirectory,
        "schematics"
    )
}
