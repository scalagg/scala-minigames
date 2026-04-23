package gg.tropic.practice.scoreboard.animation

import gg.tropic.practice.versioned.MinigameModernAccess
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

object GradientWaveTitle
{
    fun render(text: String, primaryHex: String, secondaryHex: String): String
    {
        if (!MinigameModernAccess.isModern()) {
            return text
        }

        val time = System.currentTimeMillis() / 80.0
        val primary = parse(primaryHex)
        val secondary = parse(secondaryHex)
        val builder = StringBuilder()

        text.forEachIndexed { index, character ->
            val phase = (index / 14.0) - (time * 0.07)
            val wave = (sin(phase * PI * 2.0) + 1.0) / 2.0
            builder.append(hex(lerp(primary, secondary, wave)))
            builder.append("§l")
            builder.append(character)
        }

        return builder.toString()
    }

    fun staticTitle(text: String): String
    {
        if (!MinigameModernAccess.isModern())
        {
            return text
        }

        return "§l" + MinigameModernAccess.gradient(text.uppercase(), "#d94b66", "#ffffff")
    }

    private fun hex(value: String): String
    {
        val normalized = value.removePrefix("#")
        val builder = StringBuilder("§x")
        normalized.forEach { builder.append('§').append(it) }
        return builder.toString()
    }

    private fun hex(rgb: Int): String = hex(String.format("%06X", rgb))

    private fun parse(hex: String) = hex.removePrefix("#").toInt(16)

    private fun lerp(a: Int, b: Int, t: Double): Int
    {
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF

        val red = (ar + (br - ar) * t).roundToInt()
        val green = (ag + (bg - ag) * t).roundToInt()
        val blue = (ab + (bb - ab) * t).roundToInt()
        return (red shl 16) or (green shl 8) or blue
    }
}
