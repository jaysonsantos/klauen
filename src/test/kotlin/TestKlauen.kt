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

        for (i in 0..1000) {
            worker.pushBottom(i)
        }

        for (i in 1000.downTo(0)) {
            assertEquals(Work.Data(i), stealer.steal())
        }
    }
}
