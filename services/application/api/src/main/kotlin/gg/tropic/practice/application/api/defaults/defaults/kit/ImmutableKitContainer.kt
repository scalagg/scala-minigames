package gg.tropic.practice.application.api.defaults.defaults.kit

data class ImmutableKitContainer(
    val kits: Map<String, ImmutableKit> = mapOf()
)
