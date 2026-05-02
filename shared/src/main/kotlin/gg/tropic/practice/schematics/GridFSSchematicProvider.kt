package gg.tropic.practice.schematics

import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.gridfs.model.GridFSFile
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.session.ClipboardHolder
import gg.scala.store.ScalaDataStoreShared
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.*
import java.util.concurrent.CompletableFuture

class GridFSSchematicProvider
{
    private val database: MongoDatabase = ScalaDataStoreShared.INSTANCE
        .getNewMongoConnection()
        .getConnection()
        .getDatabase("worlddb")

    private val bucket: GridFSBucket = GridFSBuckets.create(database, "schematics")

    init
    {

        database.getCollection("schematics.files")
            .createIndex(
                Indexes.ascending("filename"),
                IndexOptions().unique(true)
            )
    }

    fun saveSchematic(name: String, schematicData: ByteArray) = CompletableFuture
        .runAsync {
            deleteSchematicSync(name)
            ByteArrayInputStream(schematicData).use { inputStream ->
                bucket.uploadFromStream(name, inputStream)
            }
        }

    fun saveSchematic(name: String, schematicFile: File) = CompletableFuture
        .supplyAsync {
            readFileToBytes(schematicFile)
        }
        .thenComposeAsync { saveSchematic(name, it) }

    fun loadSchematic(name: String) = CompletableFuture
        .supplyAsync {
            if (!schematicExistsSync(name))
            {
                return@supplyAsync null
            }

            ByteArrayOutputStream().use { outputStream ->
                bucket.downloadToStream(name, outputStream)
                SchematicUtil.loadSchematicFromInputStream(
                    ByteArrayInputStream(outputStream.toByteArray())
                )
            }
        }

    fun loadSchematicBytes(name: String) = CompletableFuture
        .supplyAsync {
            if (!schematicExistsSync(name))
            {
                return@supplyAsync null
            }

            ByteArrayOutputStream().use { outputStream ->
                bucket.downloadToStream(name, outputStream)
                outputStream.toByteArray()
            }
        }

    fun schematicExists(name: String) = CompletableFuture.supplyAsync { schematicExistsSync(name) }

    fun deleteSchematic(name: String) = CompletableFuture.supplyAsync {
        deleteSchematicSync(name)
    }

    fun listSchematics() = CompletableFuture
        .supplyAsync {
            bucket.find()
                .map { it.filename }
                .toList()
        }

    fun saveFromClipboard(player: Player, name: String) = CompletableFuture.supplyAsync {
        val session = WorldEdit.getInstance().sessionManager
            .get(BukkitAdapter.adapt(player))

        val holder: ClipboardHolder = try
        {
            session.clipboard
        } catch (e: EmptyClipboardException)
        {
            return@supplyAsync false
        }

        val output = ByteArrayOutputStream()
        BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(output).use { writer ->
            writer.write(holder.clipboard)
        }

        saveSchematic(name, output.toByteArray()).join()
        true
    }

    private fun schematicExistsSync(name: String): Boolean
    {
        return try
        {
            val file: GridFSFile? = bucket.find(Filters.eq("filename", name)).first()
            file != null
        } catch (e: Exception)
        {
            false
        }
    }

    private fun deleteSchematicSync(name: String): Boolean
    {
        return try
        {
            val file: GridFSFile? = bucket.find(Filters.eq("filename", name)).first()
            if (file != null)
            {
                bucket.delete(file.objectId)
                true
            } else
            {
                false
            }
        } catch (e: Exception)
        {
            Bukkit.getLogger().warning("Error deleting schematic $name: ${e.message}")
            false
        }
    }

    private fun readFileToBytes(file: File): ByteArray
    {
        FileInputStream(file).use { fis ->
            ByteArrayOutputStream().use { baos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1)
                {
                    baos.write(buffer, 0, bytesRead)
                }
                return baos.toByteArray()
            }
        }
    }
}
