package gg.tropic.practice.friendship

import java.util.*
import java.util.concurrent.CompletableFuture

object NoOpFriendshipRequirement : FriendshipRequirement
{
    override fun existsBetween(playerOne: UUID, playerTwo: UUID) =
        CompletableFuture.completedFuture(true)
}
