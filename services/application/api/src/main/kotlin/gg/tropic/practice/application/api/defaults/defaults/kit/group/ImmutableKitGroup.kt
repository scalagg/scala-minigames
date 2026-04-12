package gg.tropic.practice.application.api.defaults.defaults.kit.group

data class ImmutableKitGroup(
    val id: String,
    val contains: MutableList<String> = mutableListOf()
)
