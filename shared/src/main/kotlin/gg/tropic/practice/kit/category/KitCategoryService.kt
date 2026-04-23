package gg.tropic.practice.kit.category

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.namespace
import gg.tropic.practice.namespaceShortened
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

@Service
object KitCategoryService : DataSyncService<KitCategoryContainer>()
{
    object CategoryKeys : DataSyncKeys
    {
        override fun newStore() = "mi-practice-kits-categories"

        override fun store() = Key.key(namespace(), "categories")
        override fun sync() = Key.key(namespaceShortened().suffixWhenDev(), "csync")
    }

    override fun locatedIn() = DataSyncSource.Mongo
    override fun keys() = CategoryKeys
    override fun type() = KitCategoryContainer::class.java

    fun categoriesOf(kit: Kit) = cached().categories.filter {
        kit.id in it.kits
    }
}
