package gg.tropic.practice.tasks

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class DistributedTaskRunnable(
    private val plugin: Plugin,
    private val tasks: List<() -> Unit>,
    private val tasksPerTick: Int = 10,
    private val onComplete: (() -> Unit)? = null
) : BukkitRunnable() {

    private var currentIndex = 0
    private var bukkitTask: BukkitTask? = null

    fun start(): BukkitTask {
        if (tasks.isEmpty()) {
            onComplete?.invoke()
            return this.runTask(plugin)
        }

        bukkitTask = this.runTaskTimer(plugin, 0L, 1L) 
        return bukkitTask!!
    }

    override fun run() {

        var processed = 0

        while (currentIndex < tasks.size && processed < tasksPerTick) {
            try {
                tasks[currentIndex]()
            } catch (e: Exception) {
                plugin.logger.warning("Error executing task at index $currentIndex: ${e.message}")
                e.printStackTrace()
            }

            currentIndex++
            processed++
        }

        if (currentIndex >= tasks.size) {
            finish()
        }
    }

    private fun finish() {
        cancel()
        onComplete?.invoke()
    }

    fun getProgress(): Double {
        return if (tasks.isEmpty()) 100.0 else (currentIndex.toDouble() / tasks.size) * 100.0
    }

    fun getRemainingTasks(): Int {
        return maxOf(0, tasks.size - currentIndex)
    }

    fun stop() {
        cancel()
    }
}

class DistributedTaskBuilder(private val plugin: Plugin) {
    private val tasks = mutableListOf<() -> Unit>()
    private var tasksPerTick = 10
    private var onComplete: (() -> Unit)? = null

    fun addTask(task: () -> Unit): DistributedTaskBuilder {
        tasks.add(task)
        return this
    }

    fun addTasks(newTasks: Collection<() -> Unit>): DistributedTaskBuilder {
        tasks.addAll(newTasks)
        return this
    }

    fun setTasksPerTick(count: Int): DistributedTaskBuilder {
        this.tasksPerTick = count
        return this
    }

    fun onComplete(callback: () -> Unit): DistributedTaskBuilder {
        this.onComplete = callback
        return this
    }

    fun build(): DistributedTaskRunnable {
        return DistributedTaskRunnable(plugin, tasks.toList(), tasksPerTick, onComplete)
    }

    fun start(): DistributedTaskRunnable {
        val runnable = build()
        runnable.start()
        return runnable
    }
}

fun Plugin.createDistributedTask(): DistributedTaskBuilder {
    return DistributedTaskBuilder(this)
}

