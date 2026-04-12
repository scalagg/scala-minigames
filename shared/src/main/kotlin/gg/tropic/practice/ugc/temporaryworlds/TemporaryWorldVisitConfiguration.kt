package gg.tropic.practice.ugc.temporaryworlds

import gg.tropic.practice.ugc.generation.visits.VisitWorldRequestConfiguration

class TemporaryWorldVisitConfiguration(
    val blockType: String
) : VisitWorldRequestConfiguration
{
    override fun getAbstractType() = TemporaryWorldVisitConfiguration::class.java
}
