package gg.tropic.practice.map.metadata.impl

import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.scala.commons.spatial.Position

data class MapEnderChestMetadata(
    override val id: String,
    var locations: Map<String, List<Position>>
) : AbstractMapMetadata()
{
    fun getByTeam(teamIdentifier: TeamIdentifier) = locations[teamIdentifier.label.lowercase()] ?: listOf()

    override fun report() = "EChests | $id | ${locations.size} group (${locations.entries.joinToString(", ") { "${it.key} -> ${it.value.size}"}})"
    override fun getAbstractType() = MapEnderChestMetadata::class.java
}
