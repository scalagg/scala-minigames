package gg.tropic.practice.kit.category

data class KitCategoryContainer(
    val categories: MutableList<KitCategory> = mutableListOf()
)
{
    fun findById(id: String) = categories.firstOrNull {
        it.id.equals(id, true)
    }
}
