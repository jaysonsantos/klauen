import java.util.concurrent.atomic.AtomicInteger

sealed class Work {
    object Empty : Work()
    object Abort : Work()
    class Data<out T>(val data: T) : Work() {
        override fun equals(other: Any?): Boolean {
            if (other == null || other.javaClass != this.javaClass)
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
        // val t = top.get()
        val b = bottom.get()
        // val size = b - t
        // Grow if size >= buffer.size - 1
        buffer.put(b, data)
        bottom.incrementAndGet()
    }

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

class Buffer<T> {
    private var list: MutableList<T> = mutableListOf() // arrayList?

    fun put(index: Int, data: T) {
        val calculatedIndex = if (index == 0) 0 else index % list.size
        if (calculatedIndex >= list.size) {
            list.add(calculatedIndex, data)
        } else
            list[calculatedIndex] = data
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
