# klauen
A work stealing queue written in Kotlin and based on Chase-Lev's paper *Dynamic Circular Work-Stealing Deque*.

## Usage

```kotlin
import klauen.create
import klauen.Work

fun main(args: Array<String>) {
    // Create a pair with a worker and a stealer for the desired generic data
    // the worker is not thread safe and it is used to push data to the bottom of the queue
    // the stealer is safe to be shared between threads and it is used to steal data from the queue
    val (worker, stealer) = create<Int>()

    // Push data to the queue
    worker.pushBottom(1)

    // Get data from the queue
    assertEquals(Work.Data(1), stealer.steal())

    // If someone steal the data from you Work.Abort is returned 
    // and it is up to you on how to retry to get more data
    assertEquals(Work.Abort, stealer.steal())

    // If the queue is empty Work.Empty is returned and it is also up to you on how to try
    // again
    assertEquals(Work.Empty, stealer.steal())
}
```
