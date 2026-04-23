package gg.tropic.practice.map.utilities

import org.bukkit.block.BlockFace
import java.util.*

object MapBlockFaceUtilities
{
    val AXIS = arrayOfNulls<BlockFace>(4)
    val RADIAL = arrayOf(
        BlockFace.WEST,
        BlockFace.NORTH_WEST,
        BlockFace.NORTH,
        BlockFace.NORTH_EAST,
        BlockFace.EAST,
        BlockFace.SOUTH_EAST,
        BlockFace.SOUTH,
        BlockFace.SOUTH_WEST
    )
    val BLOCK_SIDES =
        arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
    val ATTACHEDFACES = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP)
    val ATTACHEDFACESDOWN = BLOCK_SIDES
    private val notches = EnumMap<BlockFace, Int>(BlockFace::class.java)

    init
    {
        for (i in RADIAL.indices)
        {
            notches[RADIAL[i]] = i
        }
        for (i in AXIS.indices)
        {
            AXIS[i] = RADIAL[i shl 1]
        }
    }

    fun wrapAngle(angle: Int): Int
    {
        var wrappedAngle = angle
        while (wrappedAngle <= -180)
        {
            wrappedAngle += 360
        }
        while (wrappedAngle > 180)
        {
            wrappedAngle -= 360
        }
        return wrappedAngle
    }

    fun faceToNotch(face: BlockFace?): Int
    {
        val notch = notches[face]
        return notch ?: 0
    }

    fun faceToYaw(face: BlockFace): Int
    {
        return wrapAngle(45 * faceToNotch(face))
    }

    
    @JvmOverloads
    fun yawToFace(yaw: Float, useSubCardinalDirections: Boolean = true): BlockFace?
    {
        return if (useSubCardinalDirections)
        {
            RADIAL[Math.round(yaw / 45f) and 0x7]
        } else
        {
            AXIS[Math.round(yaw / 90f) and 0x3]
        }
    }
}
