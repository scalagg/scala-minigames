package gg.tropic.practice.minigame.rejoin

import java.util.UUID

data class RejoinToken(
    val server: String,
    val expectation: UUID,
    val expiration: Long,
    val gameDescription: String
)
