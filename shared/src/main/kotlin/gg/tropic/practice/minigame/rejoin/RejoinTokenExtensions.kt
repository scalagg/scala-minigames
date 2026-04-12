package gg.tropic.practice.minigame.rejoin

import gg.tropic.practice.namespace
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import java.util.UUID

fun UUID.toRejoinToken() = ScalaCommonsSpigot.instance
        .kvConnection
        .sync()
    .get("${namespace()}:minigames:rejoin:$this")
    ?.let { text ->
        Serializers.gson.fromJson(text, RejoinToken::class.java)
    }

fun UUID.destroyRejoinToken() = ScalaCommonsSpigot.instance
        .kvConnection
        .sync()
    .del("${namespace()}:minigames:rejoin:$this")
