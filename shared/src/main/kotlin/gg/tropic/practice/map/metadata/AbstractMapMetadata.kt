package gg.tropic.practice.map.metadata

import net.evilblock.cubed.serializers.impl.AbstractTypeSerializable

abstract class AbstractMapMetadata : AbstractTypeSerializable
{
    abstract val id: String
    abstract fun report(): String
}

