package mc.arch.minigames.parties.stream

import mc.arch.minigames.parties.model.Party
import net.evilblock.cubed.util.bukkit.FancyMessage

object PartyMessageStream
{
    fun pushToStream(
        party: Party, message: FancyMessage
    )
    {
        party.members.values.toMutableList()
            .apply { add(party.leader) }
            .forEach {
                it.sendMessage(message)
            }
    }
}
