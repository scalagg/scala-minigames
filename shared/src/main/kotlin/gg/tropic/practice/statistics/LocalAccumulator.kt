package gg.tropic.practice.statistics

import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class LocalAccumulator(
    private val owner: UUID,
    private val mappings: ConcurrentHashMap<String, Double> = ConcurrentHashMap()
)
{
    fun update(id: String, value: Int) = mappings.put(id, value.toDouble())
    fun update(id: String, value: Double) = mappings.put(id, value)

    fun reset(id: String)
    {
        mappings[id] = 0.0
    }

    @JvmOverloads
    fun increment(id: String, amount: Double = 1.0)
    {
        mappings.compute(id) { _, value ->
            val old = (value ?: 0.0)

            
            old + amount
        }
    }

    fun valueOf(id: String) = mappings.getOrPut(id) { 0.0 }
}
