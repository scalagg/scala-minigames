package mc.arch.minigames.versioned.generics

interface SlimeWorldGeneric<W : Any>
{
    val worldInstance: W
    fun clone(newName: String): SlimeWorldGeneric<W>

    fun getLastSave(): Long?
    fun updateLastSave()
}
