package gg.tropic.practice.application.api.defaults.defaults.kit.group

import gg.tropic.practice.application.api.defaults.DPSDataSync
import gg.tropic.practice.application.api.defaults.DPSDataSyncKeys
import gg.tropic.practice.application.api.defaults.DPSDataSyncSource
import gg.tropic.practice.application.api.defaults.defaults.kit.ImmutableKit
import gg.tropic.practice.namespace
import gg.tropic.practice.namespaceShortened
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

object KitGroupDataSync : DPSDataSync<ImmutableKitContainer>()
{
    object DPSKitGroupKeys : DPSDataSyncKeys
    {
        override fun newStore() = "mi-practice-kits-groups"

        override fun store() = Key.key(namespace(), "groups")
        override fun sync() = Key.key(namespaceShortened().suffixWhenDev(), "gsync")
    }

    override fun locatedIn() = DPSDataSyncSource.Mongo

    override fun keys() = DPSKitGroupKeys
    override fun type() = ImmutableKitContainer::class.java

    fun groupsOf(kit: ImmutableKit) = cached().backingGroups
        .filter {
            kit.id in it.contains
        }
}
