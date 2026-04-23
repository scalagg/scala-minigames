package gg.scala.commons.consensus.elections

import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * @author Limo
 * @date 2/3/26
 */
object LeaderElection
{
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    fun withAnyServerSyncServer(
        electionID: String,
        refreshRate: Duration,
        elect: () -> Unit,
        resign: () -> Unit
    )
    {
        elect()

        executor.scheduleAtFixedRate({
            runCatching {
                elect()
            }.onFailure {
                resign()
            }
        }, refreshRate.toMillis(), refreshRate.toMillis(), TimeUnit.MILLISECONDS)
    }
}
