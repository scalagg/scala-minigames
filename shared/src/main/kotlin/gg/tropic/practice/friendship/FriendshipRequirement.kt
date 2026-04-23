package gg.tropic.practice.friendship

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface FriendshipRequirement
{
    fun existsBetween(playerOne: UUID, playerTwo: UUID): CompletableFuture<Boolean>
}
