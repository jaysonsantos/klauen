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
    class Data<out T>(val data: T) : Work() {
        override fun equals(other: Any?): Boolean {
            if (other == null || other::class != this::class)
                return false
            other as Work.Data<*>
            return this.data == other.data
        }

        override fun hashCode(): Int {
            return data?.hashCode() ?: 0
        }
    }
}

class Deque<T> {
    private var top: AtomicInteger = AtomicInteger(0)
    private var bottom: AtomicInteger = AtomicInteger(0)
    private var buffer: Buffer<T?> = Buffer()

    fun pushBottom(data: T) {
        val t = top.get()
        val b = bottom.get()
        val size = b - t
        // If size >= buffer.size - 1 it means that there is no space left to rotate so we have to "grow" the list
        // and it is done by just by appending data
        if (size >= buffer.size())
            buffer.put(data)
        else
            buffer.put(b, data)
        bottom.incrementAndGet()
    }

    /**
     * Steal a work from buffer and it is thread safe.
     * It can return 3 result Work.Empty when the queue is empty, Work.Abort when some other thread stole your data
     * and Work.Data(T) with desired data.
     */
    fun steal(): Work {
        val t = top.get()
        val b = bottom.get()
        val i = b - t
        if (i <= 0) {
            return Work.Empty
        }
        val item = this.buffer.get(i)
        if (!top.compareAndSet(t, t + 1))
            return Work.Abort
        return Work.Data(item)
    }
}

/**
 * Buffer class which uses a list as circular array.
 */
class Buffer<T> {
    private var list: MutableList<T> = mutableListOf() // arrayList?

    /**
     * Actual size of the list.
     */
    fun size(): Int = list.size

    /**
     * Add a data to the circular list.
     */
    fun put(index: Int, data: T) {
        list[index % list.size] = data
    }

    /**
     * Append data to the list, used when there is not enough space to use it as circular.
     */
    fun put(data: T) = list.add(data)
    fun get(index: Int): T = list[index % list.size]
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
