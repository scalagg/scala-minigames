package gg.tropic.practice.minigame.levitationportals

import me.lucko.helper.Helper
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue

fun Player.removeSession()
{
    removeMetadata("levitation", Helper.hostPlugin())
}

fun Player.setSession(session: LevitationSession)
{
    setMetadata("levitation", FixedMetadataValue(Helper.hostPlugin(), session))
}

fun Player.getSession(): LevitationSession?
{
    return getMetadata("levitation").firstOrNull()?.value() as? LevitationSession
}
