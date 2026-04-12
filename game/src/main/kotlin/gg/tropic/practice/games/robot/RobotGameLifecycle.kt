package gg.tropic.practice.games.robot

import gg.tropic.practice.games.GameImpl

object RobotGameLifecycle
{
    var createNewRobotInstances: (GameImpl) -> Set<RobotInstance> = {
        throw UnsupportedOperationException("Robot integration not available")
    }
}
