package gg.tropic.practice.map.synthetics

import gg.tropic.practice.map.metadata.sign.MapSignMetadataModel
import net.evilblock.cubed.entity.hologram.HologramEntity
import java.util.UUID

data class PreparedSyntheticSignModel(
    val hologramEntity: HologramEntity,
    val signMetadataModel: MapSignMetadataModel,
    val refID: UUID = UUID.randomUUID(),
)
