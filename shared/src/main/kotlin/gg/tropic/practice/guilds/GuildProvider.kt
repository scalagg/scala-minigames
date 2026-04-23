package gg.tropic.practice.guilds

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface GuildProvider
{
    fun provideGuildNameFor(uniqueId: UUID): CompletableFuture<String?>
}
