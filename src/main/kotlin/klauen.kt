import java.util.concurrent.atomic.AtomicInteger

/**
 * Possible returns of Work.
 */
sealed class Work {
    /**
     * The queue is empty and there is no work.
     */
    object Empty : Work()

    /**
     * Someone stole the job from the queue.
     */
    object Abort : Work()

    /**
     * The actual data from the queue.
     */
    data class Data<out T>(val data: T) : Work()
}

class Deque<T> {
    private var top: AtomicInteger = AtomicInteger(0)
    private var bottom: AtomicInteger = AtomicInteger(0)
    private var buffer: Buffer<T> = Buffer(0)

    /**
     * Push items into the bottom of the queue.
     */
    fun pushBottom(data: T) {
        val t = top.get()
        val b = bottom.get()
        val size = b - t
        // If size >= buffer.size - 1 it means that there is no space left to rotate so we have to "grow" the list
        // and it is done by just by appending data
        if (size >= buffer.size)
            buffer = buffer.grow(b, t)
        buffer.put(b, data)
        bottom.incrementAndGet()
    }

    /**
     * Steal a work from the top of the buffer and it is thread safe.
     * It can return 3 result Work.Empty when the queue is empty, Work.Abort when some other thread stole your data
     * and Work.Data(T) with desired data.
     */
    fun steal(): Work {
        val t = top.get()
        val b = bottom.get()
        val size = b - t
        if (size <= 0) {
            return Work.Empty
        }
        val item = this.buffer.get(t)
        if (!top.compareAndSet(t, t + 1))
            return Work.Abort
        return Work.Data(item)
    }
}

/**
 * Buffer class which uses a list as circular list.
 * @param logSize
 */
class Buffer<T>(private val logSize: Int) {
    val size: Int = 1 shl logSize
    private val list: MutableList<T?> = mutableListOf<T?>()

    init {
        for (i in 0..size) {
            list.add(i, null)
        }
    }

    /**
     * Add a data to the circular list.
     */
    fun put(index: Int, data: T) {
        list[index % size] = data
    }

    /**
     * Get an item from the list in a specific index.
     */
    fun get(index: Int): T = list[index % size]!!

    /**
     * Grow the buffer by creating a new version of it and copying over all items.
     */
    fun grow(bottom: Int, top: Int): Buffer<T> {
        val buffer = Buffer<T>(size + 1)
        for (i in top..bottom) {
            buffer.put(i, get(i))
        }
        return buffer
    }
}

/**
 * Worker class which is not thread safe and should stay only in the task producer thread.
 */
class Worker<T>(private var deque: Deque<T>) {
    fun pushBottom(data: T) {
        deque.pushBottom(data)
    }
}

/**
 * Stealer class which is safe to be shared between threads.
 */
class Stealer<T>(private val deque: Deque<T>) {
    fun steal(): Work {
        return this.deque.steal()
    }
}

/**
 * Create a pair of worker and stealer where workers should not be shared between threads and stealers are free to be
 * shared.
 */
fun <T> create(): Pair<Worker<T>, Stealer<T>> {
    val deque = Deque<T>()
    return Pair(Worker(deque), Stealer(deque))
}
