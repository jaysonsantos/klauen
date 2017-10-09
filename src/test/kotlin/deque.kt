import org.junit.Test
import kotlin.test.assertEquals

class TestDeque {
    @Test
    fun testQueue() {
        val (worker, stealer) = create<Int>()
        assertEquals(Work.Empty(), stealer.steal())
        worker.pushBottom(1)
        assertEquals(Work.Data(1), stealer.steal())
    }
}