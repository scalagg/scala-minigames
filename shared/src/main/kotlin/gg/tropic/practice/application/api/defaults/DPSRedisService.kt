package gg.tropic.practice.application.api.defaults

import gg.scala.aware.Aware
import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.tropic.practice.suffixWhenDev
import java.util.logging.Logger

class DPSRedisService(
    private val channel: String,
    private val raw: Boolean = false
)
{
    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("${
                if (!raw) "minigames:" else ""
            }$channel".suffixWhenDev())
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    @Volatile
    private var started = false

    fun <T> configure(
        lambda: Aware<AwareMessage>.() -> T
    ): T
    {
        return this.aware.lambda()
    }

    fun createMessage(packet: String, vararg pairs: Pair<String, Any?>): AwareMessage =
        AwareMessage.of(packet, this.aware, *pairs)

    fun start()
    {
        if (started) return
        this.aware.connect()
            .toCompletableFuture()
            .join()
        started = true
    }
}
