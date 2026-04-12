package gg.tropic.practice.application.api.defaults.defaults.map

import gg.tropic.practice.application.api.defaults.DPSDataSync
import gg.tropic.practice.application.api.defaults.DPSDataSyncKeys
import gg.tropic.practice.application.api.defaults.DPSDataSyncSource
import gg.tropic.practice.application.api.defaults.defaults.kit.ImmutableKit
import gg.tropic.practice.application.api.defaults.defaults.kit.group.ImmutableKitGroup
import gg.tropic.practice.application.api.defaults.defaults.kit.group.KitGroupDataSync
import gg.tropic.practice.namespace
import gg.tropic.practice.namespaceShortened
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

object MapDataSync : DPSDataSync<ImmutableMapContainer>()
{
    object DPSMapKeys : DPSDataSyncKeys
    {
        override fun newStore() = "mi-practice-maps"

        override fun store() = Key.key(namespace(), "maps")
        override fun sync() = Key.key(namespaceShortened().suffixWhenDev(), "msync")
    }

    override fun locatedIn() = DPSDataSyncSource.Mongo

    override fun keys() = DPSMapKeys
    override fun type() = ImmutableMapContainer::class.java

    fun selectMapIfCompatible(kit: ImmutableKit, mapID: String?): ImmutableMap?
    {
        val groups = KitGroupDataSync.groupsOf(kit)
            .map(ImmutableKitGroup::id)

        return cached().maps.values
            .filterNot(ImmutableMap::locked)
            .filter { groups.intersect(it.associatedKitGroups).isNotEmpty() }
            .shuffled()
            .firstOrNull { if (mapID == null) true else it.name == mapID }
    }

    fun selectRandomMapCompatibleWith(kit: ImmutableKit): ImmutableMap?
    {
        val groups = KitGroupDataSync.groupsOf(kit)
            .map(ImmutableKitGroup::id)

        return cached().maps.values
            .filterNot(ImmutableMap::locked)
            .shuffled()
            .firstOrNull {
                groups.intersect(it.associatedKitGroups).isNotEmpty()
            }
    }
}
