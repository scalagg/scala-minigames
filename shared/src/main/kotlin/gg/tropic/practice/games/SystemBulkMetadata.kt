package gg.tropic.practice.games

import gg.tropic.practice.metadata.InstanceMetadata

data class SystemBulkMetadata(
    val indexes: Map<String, InstanceMetadata>
)
