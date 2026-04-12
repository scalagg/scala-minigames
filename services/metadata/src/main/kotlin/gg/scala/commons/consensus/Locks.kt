package gg.scala.commons.consensus

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * @author Limo
 * @date 2/3/26
 */
object Locks
{
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    fun <T> withGlobalLock(namespace: String, key: String, action: () -> T): CompletableFuture<T?>
    {
        val lockKey = "$namespace:$key"
        val lock = locks.computeIfAbsent(lockKey) { ReentrantLock() }

        return CompletableFuture.supplyAsync {
            lock.lock()
            try
            {
                action()
            }
            finally
            {
                lock.unlock()
            }
        }
    }
}
