import org.junit.Test
import kotlin.test.assertEquals

class TestDeque {
    @Test
    fun testQueue() {
        val (worker, stealer) = create<Int>()
        assertEquals(Work.Empty, stealer.steal())
        worker.pushBottom(1)
        assertEquals(Work.Data(1), stealer.steal())
        assertEquals(Work.Empty, stealer.steal())
        assertEquals(Work.Empty, stealer.steal())

        worker.pushBottom(0)
        worker.pushBottom(1)

        assertEquals(Work.Data(0), stealer.steal())
        assertEquals(Work.Data(1), stealer.steal())
        assertEquals(Work.Empty, stealer.steal())

        for (i in 0..1000) {
            worker.pushBottom(i)
        }

        assertEquals(Work.Data(0), stealer.steal())
        worker.pushBottom(-1)

        for (i in 1..999) {
            assertEquals(Work.Data(i), stealer.steal())
        }
        assertEquals(Work.Data(1000), stealer.steal())
        assertEquals(Work.Data(-1), stealer.steal())
        assertEquals(Work.Empty, stealer.steal())
    }
}
