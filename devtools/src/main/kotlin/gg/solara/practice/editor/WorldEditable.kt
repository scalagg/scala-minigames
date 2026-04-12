package gg.solara.practice.editor

import com.cryptomorin.xseries.XMaterial
import gg.solara.practice.editor.EditorGenerator.VoidWorldGenerator
import gg.solara.practice.utilities.WorldEditUtils
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipInputStream

class WorldEditable(private val file: File) : Editable
{
    override val icon: XMaterial
        get() = XMaterial.GRASS_BLOCK
    override val displayName: String
        get() = file.name.split(".").first()

    override fun prepareWorld(player: Player): World
    {

        val worldName = "editor_temp_${UUID.randomUUID()}"
        val worldContainer = Bukkit.getWorldContainer()
        val worldDir = File(worldContainer, worldName)

        worldDir.mkdirs()

        player.sendMessage("${CC.GREEN}Unzipping file...")

        unzipFile(file, worldDir)

        val worldCreator = WorldCreator.name(worldName).generator(VoidWorldGenerator).generateStructures(false)
        val newWorld = worldCreator
            .createWorld()
            .apply {
                setGameRuleValue("doMobSpawning", "false")
            }
            ?: throw RuntimeException("Failed to create world: $worldName")

        return newWorld
    }

    private fun unzipFile(zipFile: File, destDir: File)
    {
        zipFile.inputStream().use { fileInputStream ->
            ZipInputStream(fileInputStream).use { zipInputStream ->
                var entry = zipInputStream.nextEntry

                while (entry != null)
                {

                    val entryPath = entry.name
                    val pathParts = entryPath.split("/")

                    if (pathParts.size <= 1)
                    {
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                        continue
                    }

                    val adjustedPath = pathParts.drop(1).joinToString("/")
                    val entryFile = File(destDir, adjustedPath)

                    if (entry.isDirectory)
                    {

                        entryFile.mkdirs()
                    } else
                    {

                        entryFile.parentFile?.mkdirs()

                        FileOutputStream(entryFile).use { outputStream ->
                            zipInputStream.copyTo(outputStream)
                        }
                    }

                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
        }
    }
}
