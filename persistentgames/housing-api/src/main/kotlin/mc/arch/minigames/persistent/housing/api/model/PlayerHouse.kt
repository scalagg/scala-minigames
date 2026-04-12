package mc.arch.minigames.persistent.housing.api.model

import gg.scala.commons.annotations.Model
import gg.scala.store.storage.storable.IDataStoreObject
import gg.tropic.practice.ugc.generation.visits.VisitWorldRequestConfiguration
import java.util.UUID

@Model
data class PlayerHouse(
    override val identifier: UUID = UUID.randomUUID(),

) : IDataStoreObject
