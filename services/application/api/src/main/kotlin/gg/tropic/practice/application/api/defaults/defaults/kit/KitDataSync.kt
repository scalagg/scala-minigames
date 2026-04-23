package gg.tropic.practice.application.api.defaults.defaults.kit

import gg.tropic.practice.application.api.defaults.DPSDataSync
import gg.tropic.practice.application.api.defaults.DPSDataSyncKeys
import gg.tropic.practice.application.api.defaults.DPSDataSyncSource
import gg.tropic.practice.namespace
import gg.tropic.practice.namespaceShortened
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

object KitDataSync : DPSDataSync<ImmutableKitContainer>()
{
    object DPSMapKeys : DPSDataSyncKeys
    {
        override fun newStore() = "mi-practice-kits"

        override fun store() = Key.key(namespace(), "kits")
        override fun sync() = Key.key(namespaceShortened().suffixWhenDev(), "ksync")
    }

    override fun locatedIn() = DPSDataSyncSource.Mongo

    override fun keys() = DPSMapKeys
    override fun type() = ImmutableKitContainer::class.java
}
