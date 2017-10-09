import java.util.concurrent.atomic.AtomicInteger

sealed class Work {
    class Empty : Work()
    class Abort : Work()
    class Data<out T>(val data: T) : Work()
}

class Deque<T> {
    @Volatile
    var top: AtomicInteger = AtomicInteger(0)
    @Volatile
    var bottom: AtomicInteger = AtomicInteger(0)
    @Volatile private var buffer: Buffer<T?> = Buffer()

    fun pushBottom(data: T) {
        val t = top.get()
        val b = bottom.get()
        val size = b - t
        // Grow if size >= buffer.size - 1
        buffer.put(b, data)
        bottom.incrementAndGet()
    }

    fun steal(): Work {
        val t = top.get()
        val b = bottom.get()
        val i = b - t
        if (i <= 0) {
            return Work.Empty()
        }
        val item = this.buffer.get(i)
        if (!top.compareAndSet(t, t + 1))
            return Work.Abort()
        return Work.Data(item)
    }
}

class Buffer<T> {
    private var list: MutableList<T> = mutableListOf()

    fun put(index: Int, data: T) {
        list[index % list.size] = data
    }

    fun get(index: Int): T {
        return list[index % list.size]
    }
}

class Worker<T>(private var deque: Deque<T>) {
    fun pushBottom(data: T) {
        deque.pushBottom(data)
    }
}

class Stealer<T>(private val deque: Deque<T>) {
    fun steal(): Work {
        return this.deque.steal()
    }
}

fun <T> create(): Pair<Worker<T>, Stealer<T>> {
    val deque = Deque<T>()
    return Pair(Worker(deque), Stealer(deque))
}
