package gg.tropic.practice.application.api.defaults

import net.kyori.adventure.key.Key

interface DPSDataSyncKeys
{
    fun store(): Key

    fun newStore(): String
    {
        return store().asString()
    }
    fun sync(): Key

    fun keyOf(namespace: String, value: String) =
        Key.key(namespace, value)
}
