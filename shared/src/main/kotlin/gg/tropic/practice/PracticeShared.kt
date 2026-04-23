package gg.tropic.practice

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import gg.tropic.practice.map.metadata.AbstractMapMetadata
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer
import org.bukkit.potion.PotionEffectType

object PracticeShared
{

    init
    {
        Serializers.create {
            registerTypeAdapter(
                AbstractMapMetadata::class.java,
                AbstractTypeSerializer<AbstractMapMetadata>()
            )

            registerTypeAdapter(
                PotionEffectType::class.java,
                object : TypeAdapter<PotionEffectType>()
                {
                    override fun write(out: JsonWriter?, value: PotionEffectType?)
                    {
                        out?.value(value?.name)
                    }

                    override fun read(`in`: JsonReader?): PotionEffectType?
                    {
                        return PotionEffectType.getByName(`in`?.nextString())
                    }
                }
            )
        }
    }
}
