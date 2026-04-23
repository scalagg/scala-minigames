package gg.tropic.practice.application.api.defaults.defaults.map

data class ImmutableMap(
    val name: String,
    val displayName: String,
    val associatedSlimeTemplate: String,
    val associatedKitGroups: Set<String>,
    val locked: Boolean
)
