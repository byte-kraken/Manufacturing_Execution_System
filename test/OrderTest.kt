import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Order creation is actually not job of the MES, so only basic functionality will be tested.
 * */
internal class OrderTest {

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