package gg.tropic.practice.configuration.minigame

import com.cryptomorin.xseries.XSound
import me.lucko.helper.Schedulers
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.pow
import kotlin.math.min

object BezierTeleporter
{

    data class MotionProfile(
        val accelerationPhase: Double = 0.3,  
        val decelerationPhase: Double = 0.3,  
        val constantPhase: Double = 0.4       
    ) {
        init {
            require(accelerationPhase + decelerationPhase + constantPhase == 1.0) {
                "Motion profile phases must sum to 1.0"
            }
        }
    }

    fun teleportPlayer(
        player: Player,
        start: Location,
        end: Location,
        duration: Int = 60, 
        height: Double = 5.0,
        motionProfile: MotionProfile = MotionProfile(),
        onComplete: (() -> Unit)? = null
    )
    {

        if (start.world != end.world)
        {
            throw IllegalArgumentException("Start and end locations must be in the same world")
        }

        val controlPoints = generateBezierControlPoints(start, end, height)
        var previousPosition = start.toVector()
        var currentTick = 0
        var lastFlapTick = 0

        val accelEndTick = (duration * motionProfile.accelerationPhase).toInt()
        val constantEndTick = (duration * (motionProfile.accelerationPhase + motionProfile.constantPhase)).toInt()

        Schedulers.sync().runRepeating({ task ->
            if (!player.isOnline)
            {
                task.closeAndReportException()
                return@runRepeating
            }

            lastFlapTick += 1
            if (lastFlapTick == 5)
            {
                XSound.ENTITY_ENDER_DRAGON_FLAP.play(player)
                lastFlapTick = 0 
            }

            if (currentTick >= duration)
            {
                onComplete?.invoke()
                task.closeAndReportException()
                return@runRepeating
            }

            val curveProgress = calculateTrapezoidalProgress(
                currentTick,
                duration,
                motionProfile,
                accelEndTick,
                constantEndTick
            )

            val currentPos = calculateBezierPosition(controlPoints, curveProgress)
            val velocity = currentPos.clone().subtract(previousPosition)

            val smoothedVelocity = smoothVelocity(velocity, currentTick, duration)
            player.velocity = smoothedVelocity

            previousPosition = currentPos.clone()
            currentTick++
        }, 0L, 1L)
    }

    private fun calculateTrapezoidalProgress(
        currentTick: Int,
        totalDuration: Int,
        profile: MotionProfile,
        accelEndTick: Int,
        constantEndTick: Int
    ): Double
    {
        val normalizedTime = currentTick.toDouble() / totalDuration.toDouble()

        return when {

            currentTick <= accelEndTick -> {
                val accelProgress = currentTick.toDouble() / accelEndTick.toDouble()
                val accelContribution = profile.accelerationPhase * (accelProgress * accelProgress)
                accelContribution
            }

            currentTick <= constantEndTick -> {
                val constantProgress = (currentTick - accelEndTick).toDouble() / (constantEndTick - accelEndTick).toDouble()
                val constantContribution = profile.constantPhase * constantProgress
                profile.accelerationPhase + constantContribution
            }

            else -> {
                val decelTicks = totalDuration - constantEndTick
                val decelProgress = (currentTick - constantEndTick).toDouble() / decelTicks.toDouble()
                val decelContribution = profile.decelerationPhase * (2 * decelProgress - decelProgress * decelProgress)
                profile.accelerationPhase + profile.constantPhase + decelContribution
            }
        }
    }

    private fun smoothVelocity(velocity: Vector, currentTick: Int, totalDuration: Int): Vector
    {
        val progress = currentTick.toDouble() / totalDuration.toDouble()

        val smoothingFactor = when {
            progress < 0.1 -> progress * 10.0 
            progress > 0.9 -> (1.0 - progress) * 10.0 
            else -> 1.0
        }

        return velocity.multiply(smoothingFactor)
    }

    private fun generateBezierControlPoints(
        start: Location,
        end: Location,
        arcHeight: Double
    ): Array<Vector>
    {
        val startVec = start.toVector()
        val endVec = end.toVector()

        val midpoint = startVec.clone().add(endVec).multiply(0.5)
        val direction = endVec.clone().subtract(startVec).normalize()
        val distance = startVec.distance(endVec)

        val heightOffset = Vector(0.0, arcHeight, 0.0)

        val controlOffset = min(distance * 0.25, 3.0)

        val p0 = startVec
        val p1 = startVec.clone()
            .add(direction.clone().multiply(controlOffset))
            .add(heightOffset.clone().multiply(0.7))
        val p2 = endVec.clone()
            .subtract(direction.clone().multiply(controlOffset))
            .add(heightOffset.clone().multiply(0.7))
        val p3 = endVec

        return arrayOf(p0, p1, p2, p3)
    }

    private fun calculateBezierPosition(controlPoints: Array<Vector>, t: Double): Vector
    {
        val clampedT = t.coerceIn(0.0, 1.0)

        val p0 = controlPoints[0]
        val p1 = controlPoints[1]
        val p2 = controlPoints[2]
        val p3 = controlPoints[3]

        val oneMinusT = 1.0 - clampedT
        val oneMinusTSquared = oneMinusT.pow(2)
        val oneMinusTCubed = oneMinusT.pow(3)
        val tSquared = clampedT.pow(2)
        val tCubed = clampedT.pow(3)

        return p0.clone().multiply(oneMinusTCubed)
            .add(p1.clone().multiply(3 * oneMinusTSquared * clampedT))
            .add(p2.clone().multiply(3 * oneMinusT * tSquared))
            .add(p3.clone().multiply(tCubed))
    }
}

fun Player.teleportWithVelocity(
    destination: Location,
    duration: Int = 60,
    height: Double = 5.0,
    motionProfile: BezierTeleporter.MotionProfile = BezierTeleporter.MotionProfile(),
    onComplete: (() -> Unit)? = null
) = BezierTeleporter.teleportPlayer(
    this, this.location, destination, duration, height, motionProfile, onComplete
)

fun Player.teleportWithVelocityPreset(
    destination: Location,
    duration: Int = 60,
    height: Double = 5.0,
    preset: MotionPreset = MotionPreset.BALANCED,
    onComplete: (() -> Unit)? = null
) {
    val profile = when (preset) {
        MotionPreset.QUICK_START -> BezierTeleporter.MotionProfile(0.2, 0.4, 0.4)
        MotionPreset.SMOOTH_LANDING -> BezierTeleporter.MotionProfile(0.4, 0.2, 0.4)
        MotionPreset.BALANCED -> BezierTeleporter.MotionProfile(0.3, 0.3, 0.4)
        MotionPreset.DRAMATIC -> BezierTeleporter.MotionProfile(0.5, 0.1, 0.4)
    }

    BezierTeleporter.teleportPlayer(this, this.location, destination, duration, height, profile, onComplete)
}

enum class MotionPreset {
    QUICK_START,    
    SMOOTH_LANDING, 
    BALANCED,       
    DRAMATIC        
}
