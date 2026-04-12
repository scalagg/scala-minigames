package gg.tropic.practice.queue

import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

data class InternalQueueEntry<T>(
    val id: UUID = UUID.randomUUID(),
    val data: T,
    val timestamp: Long = System.currentTimeMillis()
)

class InternalQueue<T> {
    private val lock = ReentrantReadWriteLock()
    private val queue = LinkedList<InternalQueueEntry<T>>()
    private val uuidToEntry = HashMap<UUID, InternalQueueEntry<T>>()

    fun add(data: T): UUID {
        val entry = InternalQueueEntry(data = data)
        lock.write {
            queue.addLast(entry)
            uuidToEntry[entry.id] = entry
        }
        return entry.id
    }

    fun add(entry: InternalQueueEntry<T>): UUID {
        lock.write {
            queue.addLast(entry)
            uuidToEntry[entry.id] = entry
        }
        return entry.id
    }

    fun takeFirst(): InternalQueueEntry<T>? {
        return lock.write {
            if (queue.isEmpty()) {
                null
            } else {
                val entry = queue.removeFirst()
                uuidToEntry.remove(entry.id)
                entry
            }
        }
    }

    fun remove(uuid: UUID): Boolean {
        return lock.write {
            val entry = uuidToEntry[uuid]
            if (entry != null) {
                queue.remove(entry)
                uuidToEntry.remove(uuid)
                true
            } else {
                false
            }
        }
    }

    fun remove(entry: InternalQueueEntry<T>): Boolean {
        return remove(entry.id)
    }

    fun listElements(): List<InternalQueueEntry<T>> {
        return lock.read {
            queue.toList()
        }
    }

    fun getPosition(uuid: UUID): Int {
        return lock.read {
            val entry = uuidToEntry[uuid]
            if (entry != null) {
                queue.indexOf(entry)
            } else {
                -1
            }
        }
    }

    fun getPosition(entry: InternalQueueEntry<T>): Int {
        return getPosition(entry.id)
    }

    fun getEntry(uuid: UUID): InternalQueueEntry<T>? {
        return lock.read {
            uuidToEntry[uuid]
        }
    }

    fun size(): Int {
        return lock.read {
            queue.size
        }
    }

    fun isEmpty(): Boolean {
        return lock.read {
            queue.isEmpty()
        }
    }

    fun peekFirst(): InternalQueueEntry<T>? {
        return lock.read {
            queue.firstOrNull()
        }
    }

    fun peekLast(): InternalQueueEntry<T>? {
        return lock.read {
            queue.lastOrNull()
        }
    }

    fun clear() {
        lock.write {
            queue.clear()
            uuidToEntry.clear()
        }
    }

    fun contains(uuid: UUID): Boolean {
        return lock.read {
            uuidToEntry.containsKey(uuid)
        }
    }

    fun contains(entry: InternalQueueEntry<T>): Boolean {
        return contains(entry.id)
    }

    override fun toString(): String {
        return lock.read {
            "LocalQueue(size=${queue.size}, entries=${queue.map { "${it.id}:${it.data}" }})"
        }
    }
}
