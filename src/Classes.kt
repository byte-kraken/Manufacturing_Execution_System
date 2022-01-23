import Converter.Companion.secondsToMillis
import java.sql.Timestamp
import kotlin.random.Random

data class Order(
    var id: Int = -1,
    val products: List<Product>,
    var status: OrderStatus = OrderStatus.PAID
) {
    var priority: Int = calculateInitialPriority()
    var estimatedTimeShipping: Timestamp = calculateMinimumTimeOfShipping()

    /** Assuming full parallelization is possible. */
    fun calculateMinimumTimeOfShipping() =
        if (products.isNotEmpty())
            Timestamp(
                System.currentTimeMillis() +
                        products.maxOf { product -> product.recipe.steps.maxOf { it.durationInSec } }.secondsToMillis()
            )
        else Timestamp(System.currentTimeMillis())

    /** More products should increase the priority. */
    private fun calculateInitialPriority() = products.sumOf { it.priority }

    companion object {
        fun getRandomDummyOrder(): Order {
            val products = List(Random.nextInt(1, 5)) {
                (Product.getDummyProducts()[Random.nextInt(0, Product.getDummyProducts().size)])
            }
            return Order(products = products)
        }
    }
}

enum class OrderStatus {
    PAID, WAITING, SCHEDULED, NOT_DELIVERABLE;
}

data class Product(var id: Int = -1, val name: String, val recipe: Recipe, var priority: Int = 1) {

    companion object {
        val loveBurger = Product(
            -1,
            "Love Burger", Recipe(
                listOf(
                    RecipeStep(Procedure.BAKE, Ingredient.BREAD, 120),
                    RecipeStep(Procedure.CUT, Ingredient.TOMATO, 20),
                    RecipeStep(Procedure.FRY, Ingredient.VEGGIE_PATTY, 20),
                    RecipeStep(Procedure.CUDDLE_WITH, Ingredient.SALAD, 300),
                )
            ), 10
        )
        val metalBurger = Product(
            -1,
            "Metal Burger", Recipe(
                listOf(
                    RecipeStep(Procedure.BAKE, Ingredient.BREAD, 820),
                    RecipeStep(Procedure.NOP, Ingredient.TOMATO, 0),
                    RecipeStep(Procedure.FRY, Ingredient.STEAK, 700),
                    RecipeStep(Procedure.SCREAM_AT, Ingredient.SALAD, 1200),
                )
            ), 5
        )
        val surprisingProduct = getRandomDummyProduct("Surprising Sandwish")
        val sandySandwish = getRandomDummyProduct("Sandy Sandwish")
        val signatureSandwish = getRandomDummyProduct()


        fun getRandomDummyProduct(name: String = "Signature Sandwish"): Product {
            val steps = List(Random.nextInt(1, 5)) {
                RecipeStep(Procedure.getRandom(), Ingredient.getRandom(), Random.nextInt(0, 1000))
            }
            return Product(-1, name, Recipe(steps), Random.nextInt(1, 20))
        }

        fun getDummyProducts(): List<Product> {
            return listOf(
                loveBurger,
                metalBurger,
                surprisingProduct,
                sandySandwish,
                signatureSandwish
            )
        }
    }
}

enum class Ingredient {
    BREAD, TOMATO, SALAD, STEAK, VEGGIE_PATTY;

    companion object {
        fun getRandom(): Ingredient {
            return values().random()
        }

        fun serialize(ingredients: List<Ingredient>): String {
            return ingredients.joinToString { "," }
        }

        fun deserialize(ingredients: String): List<Ingredient> {
            return ingredients.split(",".toRegex()).map { valueOf(it) }.toList()
        }
    }
}

data class Machine(
    var id: Int,
    val name: String,
    val procedures: List<Procedure>,
    var occupiedUntil: Timestamp = Timestamp(0),
    val status: MachineStatus = MachineStatus.WORKING
) {
    companion object {
        fun getDummyMachine(): Machine {
            val procedures = List(Random.nextInt(1, 6)) {
                Procedure.getRandom()
            }
            return Machine(
                -1,
                "Magical Machine",
                procedures,
                Timestamp(System.currentTimeMillis()),
                if (Random.nextDouble(0.0, 1.0) < 0.9) MachineStatus.WORKING else MachineStatus.BROKEN
            )
        }

        fun getDummyMachines(): List<Machine> = listOf(
            *(List(Random.nextInt(5, 10)) { getDummyMachine() }.toTypedArray()),
            Machine(-1, "Cute Frying-Pan", listOf(Procedure.FRY, Procedure.CUDDLE_WITH)),
            Machine(-1, "Aggressive Oven", listOf(Procedure.SCREAM_AT, Procedure.THROW_ON_FLOOR, Procedure.BAKE)),
        )
    }
}

enum class MachineStatus {
    WORKING, BROKEN;
}


enum class Procedure {
    NOP, BAKE, FRY, LICK, CUT, SCREAM_AT, CUDDLE_WITH, JUGGLE, THROW_ON_FLOOR, ASSEMBLE;

    companion object {
        fun getRandom(): Procedure {
            return values().random()
        }
    }
}

data class Instruction(
    var id: Int,
    val order: Order,
    val product: Product,
    val machine: Machine,
    val procedure: Procedure,
    val ingredients: List<Ingredient>,
    val durationInSec: Int,
)

data class Recipe(val steps: List<RecipeStep>) {
    fun serialize(): String {
        return steps.joinToString(separator = ",") { it.serialize() }
    }

    companion object {
        fun deserialize(serializedString: String): Recipe {
            return Recipe(serializedString.split(",".toRegex()).map { RecipeStep.deserialize(it) }.toList())
        }
    }
}

data class RecipeStep(
    val procedure: Procedure = Procedure.NOP,
    val ingredients: List<Ingredient>,
    val durationInSec: Int
) {
    constructor(procedure: Procedure = Procedure.NOP, ingredient: Ingredient, duration: Int) :
            this(procedure, listOf(ingredient), duration)

    fun serialize(): String {
        return "$procedure:${ingredients.joinToString(separator = "-")}:$durationInSec"
    }

    companion object {
        fun deserialize(serializedString: String): RecipeStep {
            val strings = serializedString.split(":".toRegex()).toTypedArray()
            return RecipeStep(
                Procedure.valueOf(strings[0]),
                strings[1].split("-".toRegex()).map { Ingredient.valueOf(it) }.toList(),
                strings[2].toInt()
            )
        }
    }
}

class Converter {
    companion object {
        fun Int.secondsToMillis(): Long {
            return this.toLong() * 1000
        }

        fun Long.millisToSeconds(): Int {
            return (this / 1000).toInt()
        }
    }
}

