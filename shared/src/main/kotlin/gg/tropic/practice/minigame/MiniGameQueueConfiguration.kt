package gg.tropic.practice.minigame

data class MiniGameQueueConfiguration(
    val requiredMapID: String? = null,
    val excludeMiniInstance: String? = null,
    var bracket: String? = null
)
