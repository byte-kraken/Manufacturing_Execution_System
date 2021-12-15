import java.sql.Timestamp
import kotlin.random.Random

data class Order(
    var id: Int = -1,
    val products: List<Product>,
    var status: OrderStatus = OrderStatus.WAITING
) {
    var priority: Int = products.sumOf { it.priority }

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
    WAITING, SCHEDULED, NOT_DELIVERABLE;
}

data class Product(val name: String, val recipe: Recipe, var priority: Int = 1) {

    companion object {
        fun getRandomDummyProduct(name: String = "Signature Sandwish"): Product {
            val steps = List(Random.nextInt(1, 5)) {
                RecipeStep(Procedure.getRandom(), Ingredient.getRandom(), Random.nextInt(0, 1000))
            }
            return Product(name, Recipe(steps), Random.nextInt(1, 20))
        }

        fun getDummyProducts(): List<Product> {
            return listOf(
                Product(
                    "Love Burger", Recipe(
                        listOf(
                            RecipeStep(Procedure.BAKE, Ingredient.BREAD, 120),
                            RecipeStep(Procedure.CUT, Ingredient.TOMATO, 20),
                            RecipeStep(Procedure.FRY, Ingredient.VEGGIE_PATTY, 20),
                            RecipeStep(Procedure.CUDDLE_WITH, Ingredient.SALAD, 300),
                        )
                    ), 10
                ),
                Product(
                    "Metal Burger", Recipe(
                        listOf(
                            RecipeStep(Procedure.BAKE, Ingredient.BREAD, 820),
                            RecipeStep(Procedure.NOP, Ingredient.TOMATO, 0),
                            RecipeStep(Procedure.FRY, Ingredient.STEAK, 0),
                            RecipeStep(Procedure.SCREAM_AT, Ingredient.SALAD, 300),
                        )
                    ), 5
                ),
                getRandomDummyProduct("Surprising Sandwish"),
                getRandomDummyProduct("Sandy Sandwish"),
                getRandomDummyProduct()
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
    var occupiedUntil: Timestamp = Timestamp(System.currentTimeMillis()),
    val status: MachineStatus = MachineStatus.WORKING
) {
    companion object {
        fun getDummyMachine(): Machine {
            val procedures = List(Random.nextInt(1, 5)) {
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

        fun getDummyMachines(): List<Machine> = List(Random.nextInt(7, 12)) { getDummyMachine() }
    }
}

enum class MachineStatus {
    WORKING, BROKEN;

    companion object {
        fun getRandom(): MachineStatus {
            return values().random()
        }
    }
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
    val duration: Int
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

data class RecipeStep(val procedure: Procedure = Procedure.NOP, val ingredients: List<Ingredient>, val duration: Int) {
    constructor(procedure: Procedure = Procedure.NOP, ingredient: Ingredient, duration: Int) :
            this(procedure, listOf(ingredient), duration)

    fun serialize(): String {
        return "$procedure:${ingredients.joinToString(separator = "-")}:$duration"
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
