import java.sql.Timestamp

val loveProduct = Product(
    -1,
    "Love Burger", Recipe(
        listOf(
            RecipeStep(Procedure.CUDDLE_WITH, Ingredient.SALAD, 300),
            RecipeStep(Procedure.BAKE, Ingredient.BREAD, 120),
            RecipeStep(Procedure.CUT, Ingredient.TOMATO, 20),
            RecipeStep(Procedure.FRY, Ingredient.VEGGIE_PATTY, 20),
        )
    ), 10
)
val hateProduct = Product(
    -1,
    "Metal Burger", Recipe(
        listOf(
            RecipeStep(Procedure.SCREAM_AT, Ingredient.SALAD, 1200),
            RecipeStep(Procedure.BAKE, Ingredient.BREAD, 820),
            RecipeStep(Procedure.FRY, Ingredient.STEAK, 700),
        )
    ), 5
)
val adventureProduct = Product(
    -1,
    "Adventure Sandwich", Recipe(
        listOf(
            RecipeStep(Procedure.JUGGLE, Ingredient.TOMATO, 1000),
            RecipeStep(Procedure.THROW_ON_FLOOR, Ingredient.STEAK, 20),
            RecipeStep(Procedure.LICK, Ingredient.SALAD, 20),
        )
    ), 1
)

/** NOP, prio 10 */
val simpleProduct = Product(
    -1,
    "Tomato", Recipe(
        listOf(
            RecipeStep(Procedure.NOP, Ingredient.TOMATO, 600),
        )
    ), 10
)

/** BAKE, prio 20 */
val simpleProduct2 = Product(
    -1,
    "Toast", Recipe(
        listOf(
            RecipeStep(Procedure.BAKE, Ingredient.BREAD, 1000),
        )
    ), 20
)


val order1: Order = Order(
    products = listOf(
        loveProduct,
        hateProduct
    )
)
val order2: Order = Order(
    products = listOf(
        adventureProduct
    )
)

/** simple product */
val simpleOrder: Order = Order(
    products = listOf(
        simpleProduct
    )
)
val simpleOrderCopy = simpleOrder.copy()

/** simple product 1, 2 */
val mediumOrder: Order = Order(
    products = listOf(
        simpleProduct,
        simpleProduct2
    )
)

/** simple product 1, 2, 1 */
val hardOrder: Order = Order(
    products = listOf(
        simpleProduct,
        simpleProduct2,
        simpleProduct
    )
)


/** BAKE, FRY, CUT, ASSEMBLE */
val workingAmazingMachine = Machine(
    -1,
    "Amazing Machine",
    listOf(Procedure.BAKE, Procedure.FRY, Procedure.CUT, Procedure.ASSEMBLE),
    Timestamp(System.currentTimeMillis()),
    MachineStatus.WORKING
)

/** JUGGLE, NOP */
val workingPointlessMachine = Machine(
    -1,
    "Working pointless Machine",
    listOf(Procedure.JUGGLE, Procedure.NOP),
    Timestamp(System.currentTimeMillis()),
    MachineStatus.WORKING
)
val workingPointlessMachineCopy = workingPointlessMachine.copy()

val brokenPointlessMachine = Machine(
    -1,
    "Broken pointless Machine",
    listOf(Procedure.JUGGLE, Procedure.NOP),
    Timestamp(System.currentTimeMillis()),
    MachineStatus.BROKEN
)