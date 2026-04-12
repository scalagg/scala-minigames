package gg.solara.practice.editor

import gg.tropic.practice.namespace
import net.evilblock.cubed.ScalaCommonsSpigot

object EditorStatusMarker
{
    fun markAsComplete(world: String) = ScalaCommonsSpigot.instance.kvConnection
        .sync()
        .sadd("${namespace()}:devtools:complete", world)

    fun markAsInComplete(world: String) = ScalaCommonsSpigot.instance.kvConnection
        .sync()
        .srem("${namespace()}:devtools:complete", world)

    fun isComplete(world: String) = ScalaCommonsSpigot.instance.kvConnection
        .sync()
        .sismember("${namespace()}:devtools:complete", world)
}
