package gg.tropic.practice.quests.model

import gg.scala.game.extensions.economy.Accounts
import gg.scala.game.extensions.economy.EconomyDataSync
import gg.scala.game.extensions.economy.Transaction
import gg.scala.game.extensions.economy.TransactionService
import gg.scala.game.extensions.economy.TransactionType
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import java.util.UUID

data class QuestReward(
    var economyID: String = "coins",
    var amount: Long = 1000L
)
{
    fun toFancy(): String
    {
        val economy = EconomyDataSync.cached().economies[economyID]!!
        return "${CC.D_GRAY}+${economy.currency.color}${
            Numbers.format(amount)
        }${CC.GRAY} ${
            economy.currency.name
        }"
    }

    fun reward(player: UUID) = TransactionService
        .submit(Transaction(
            sender = Accounts.SERVER,
            receiver = player,
            type = TransactionType.Deposit,
            economy = economyID,
            amount = amount
        ))
}
