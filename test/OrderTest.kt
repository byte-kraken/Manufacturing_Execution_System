import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Order creation is actually not job of the MES, so only basic functionality will be tested.
 * */
internal class OrderTest {

    @Test
    fun calculateInitialPriority() {
        var actual = order1.calculateInitialPriority()
        var expected = 15
        assertEquals(expected, actual)
        actual = order2.calculateInitialPriority()
        expected = 1
        assertEquals(expected, actual)
    }

    @Test
    fun calculateMinimumTimeOfShipping() {
        var actual: Long = order1.calculateMinimumTimeOfShipping().time - System.currentTimeMillis()
        var expectedSeconds = 1200
        var expected: Long = (expectedSeconds * 1000).toLong()
        assertEquals(expected, actual)

        actual = order2.calculateMinimumTimeOfShipping().time - System.currentTimeMillis()
        expectedSeconds = 1000
        expected = (expectedSeconds * 1000).toLong()
        assertEquals(expected, actual)
    }
}