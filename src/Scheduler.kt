import java.sql.SQLException
import java.sql.Timestamp
import kotlin.random.Random

fun main() {
    val db = DBManager()
    db.openConnection()
    try {
        db.initializeTables()
        db.populateTablesWithDummyData()
        
        println("\n---------- Starting MES ----------")
        var count = 0
        while (true) {
            val order = db.fetchNextOrder()
            if (order != null) {
                if (schedule(order, db)) {
                    println("Order ${order.id} was scheduled successfully!")
                    db.updateOrderStatus(order.id, OrderStatus.SCHEDULED)
                } else {
                    println("Order ${order.id} was NOT scheduled successfully.")
                    db.updateOrderStatus(order.id, OrderStatus.NOT_DELIVERABLE)
                }
                db.increaseOrderPriority()
            } else {
                Thread.sleep(2000)
                count++
                if (count > Random.nextInt(1, 3)) {
                    db.populateOrdersWithDummyData()
                    count = 0
                }
            }
        }
        db.closeConnection()
    } catch (e: SQLException) {
        e.printStackTrace()
    }
}

fun schedule(order: Order, db: DBManager): Boolean {
    println("\nScheduling order ${order.id} (${order.priority}) with the following products: ${order.products.map { it.name }}")

    var instruction: Instruction
    order.products.forEach { product ->
        println(" - Analyzing recipe for ${product.name}")
        product.recipe.steps.forEach { recipeStep ->
            val machine = db.fetchMachine(recipeStep.procedure) ?: run {
                println(" ## No working machine for ${recipeStep.procedure} in ${product.name} was found, cancelling order.")
                return false
            }
            machine.occupiedUntil = Timestamp(System.currentTimeMillis() + recipeStep.duration.toLong())
            db.updateMachineOccupation(machine.id, machine.occupiedUntil)
            instruction = Instruction(
                -1,
                order,
                product,
                machine,
                recipeStep.procedure,
                recipeStep.ingredients,
                recipeStep.duration
            )
            db.addInstruction(instruction)
        }
    }
    return true
}