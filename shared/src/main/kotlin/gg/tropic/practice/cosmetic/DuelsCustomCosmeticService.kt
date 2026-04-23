package gg.tropic.practice.cosmetic

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import gg.tropic.practice.namespace
import gg.tropic.practice.namespaceShortened
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

@Service
object DuelsCustomCosmeticService : DataSyncService<DuelsCustomCosmeticContainer>()
{
    object CosmeticKeys : DataSyncKeys
    {
        override fun newStore() = "mi-practice-duels-cosmetics"

        override fun store() = Key.key(namespace(), "duels-cosmetics")
        override fun sync() = Key.key(namespaceShortened().suffixWhenDev(), "duels-cosmetics-sync")
    }

    override fun locatedIn() = DataSyncSource.Mongo
    override fun keys() = CosmeticKeys
    override fun type() = DuelsCustomCosmeticContainer::class.java
}
