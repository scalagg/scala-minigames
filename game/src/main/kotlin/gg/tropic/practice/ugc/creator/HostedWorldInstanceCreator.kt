package gg.tropic.practice.ugc.creator

import gg.tropic.practice.ugc.HostedWorldInstance
import gg.tropic.practice.ugc.WorldInstanceProviderType
import gg.tropic.practice.ugc.generation.visits.VisitWorldRequest
import gg.tropic.practice.ugc.resources.HostedWorldInstancePlayerResources
import java.util.concurrent.CompletableFuture

interface HostedWorldInstanceCreator<R : HostedWorldInstancePlayerResources>
{
    val type: WorldInstanceProviderType

    fun createInstance(request: VisitWorldRequest): CompletableFuture<HostedWorldInstance<R>>
}
