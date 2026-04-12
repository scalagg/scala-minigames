package gg.tropic.practice.metadata

object Metadata
{
    private val writer by lazy { MetadataWriter() }
    private val reader by lazy { MetadataReader() }

    fun reader() = reader
    fun writer() = writer
}
