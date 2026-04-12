package gg.tropic.practice.guilds

import java.util.*
import java.util.concurrent.CompletableFuture

object NoOpGuildProvider : GuildProvider
{
    override fun provideGuildNameFor(uniqueId: UUID): CompletableFuture<String?> =
        CompletableFuture.completedFuture(null)
}
