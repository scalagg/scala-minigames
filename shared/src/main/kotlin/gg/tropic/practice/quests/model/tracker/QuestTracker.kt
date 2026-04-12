package gg.tropic.practice.quests.model.tracker

data class QuestTracker(
    val questID: String,
    var state: QuestTrackerState
)
