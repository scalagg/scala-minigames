package gg.tropic.practice.queue.variants.robot

import gg.tropic.practice.games.bots.extractBotGameMetadata
import gg.tropic.practice.namespace
import gg.tropic.practice.persistence.RedisShared
import java.util.UUID

fun getBotMetadataOfPlayer(player: UUID) = RedisShared.keyValueCache.sync()
    .get(
        "${namespace()}:bot-metadata:$player"
    )
    ?.extractBotGameMetadata()
