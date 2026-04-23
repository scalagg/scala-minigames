package gg.tropic.practice.minigame

import gg.tropic.practice.ugc.generation.visits.VisitWorldRequestConfiguration
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer

object MiniGameSerializers
{
    fun configure()
    {
        Serializers.create {
            registerTypeAdapter(
                MiniGameConfiguration::class.java,
                AbstractTypeSerializer<MiniGameConfiguration>()
            )

            registerTypeAdapter(
                VisitWorldRequestConfiguration::class.java,
                AbstractTypeSerializer<VisitWorldRequestConfiguration>()
            )
        }
    }
}
