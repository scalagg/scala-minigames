package gg.tropic.practice.schematics

import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.gridfs.model.GridFSFile
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.sk89q.jnbt.NBTOutputStream
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extent.clipboard.io.SchematicWriter
import gg.scala.store.ScalaDataStoreShared
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.*
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPOutputStream

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
        val session = WorldEdit.getInstance().getSession(player.name)
        if (session.clipboard == null)
        {
            return@supplyAsync false
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
        val nbtOutputStream = NBTOutputStream(gzipOutputStream)

        val writer = SchematicWriter(nbtOutputStream)
        writer.write(session.clipboard.clipboard, session.selectionWorld.worldData)
        writer.close()

        gzipOutputStream.close()
        saveSchematic(name, byteArrayOutputStream.toByteArray()).join()
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
