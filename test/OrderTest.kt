import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Order creation is actually not job of the MES, so only basic functionality will be tested.
 * */
internal class OrderTest {
    private val product1 = Product(
        "Love Burger", Recipe(
            listOf(
                RecipeStep(Procedure.BAKE, Ingredient.BREAD, 120),
                RecipeStep(Procedure.CUT, Ingredient.TOMATO, 20),
                RecipeStep(Procedure.FRY, Ingredient.VEGGIE_PATTY, 20),
                RecipeStep(Procedure.CUDDLE_WITH, Ingredient.SALAD, 300),
            )
        ), 10
    )
    private val product2 = Product(
        "Metal Burger", Recipe(
            listOf(
                RecipeStep(Procedure.BAKE, Ingredient.BREAD, 820),
                RecipeStep(Procedure.NOP, Ingredient.TOMATO, 0),
                RecipeStep(Procedure.FRY, Ingredient.STEAK, 700),
                RecipeStep(Procedure.SCREAM_AT, Ingredient.SALAD, 1200),
            )
        ), 5
    )
    private val product3 = Product(
        "Adventure Burger", Recipe(
            listOf(
                RecipeStep(Procedure.BAKE, Ingredient.BREAD, 10),
                RecipeStep(Procedure.JUGGLE, Ingredient.TOMATO, 1000),
                RecipeStep(Procedure.THROW_ON_FLOOR, Ingredient.STEAK, 20),
                RecipeStep(Procedure.LICK, Ingredient.SALAD, 20),
            )
        ), 1
    )

    private val order1: Order = Order(
        products = listOf(
            product1,
            product2
        )
    )
    private val order2: Order = Order(
        products = listOf(
            product3
        )
    )

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