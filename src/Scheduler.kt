import RecipeStep.Companion.secondsToMillis
import java.sql.SQLException
import java.sql.Timestamp

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
                    db.updateOrderStatus(order.id, OrderStatus.SCHEDULED)
                    println("Order ${order.id} was scheduled successfully. Estimated time of shipping: ${order.estimatedTimeShipping}")
                } else {
                    println("Order ${order.id} was NOT scheduled successfully.")
                    db.updateOrderStatus(order.id, OrderStatus.NOT_DELIVERABLE)
                }
                db.increaseOrderPriority()
            } else {
                Thread.sleep(2000)
                count++
                if (count > 3) {
                    printMachineOccupation(db)
                    Thread.sleep(2000)
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
    println("\nScheduling order ${order.id} (Prio: ${order.priority}) with the following products: ${order.products.map { it.name }}")

    var latestScheduledEndOfProcedure = Timestamp(0)

    var instruction: Instruction
    order.products.forEach { product ->
        println(" - Analyzing recipe for ${product.name}")
        product.recipe.steps.forEach { recipeStep ->
            val machine = db.fetchMachine(recipeStep.procedure) ?: run {
                println(" ## No working machine for ${recipeStep.procedure} in ${product.name} was found, cancelling order.")
                // it would probably make sense to remove previous instructions in this case
                return false
            }
            val laterStartOfNewProcedure = maxOf(machine.occupiedUntil.time, System.currentTimeMillis())
            machine.occupiedUntil = Timestamp(laterStartOfNewProcedure + recipeStep.durationInSec.secondsToMillis())
            db.updateMachineOccupation(machine.id, machine.occupiedUntil)
            val prev = latestScheduledEndOfProcedure
            latestScheduledEndOfProcedure =
                Timestamp(maxOf(latestScheduledEndOfProcedure.time, machine.occupiedUntil.time))

            instruction = Instruction(
                -1,
                order,
                product,
                machine,
                recipeStep.procedure,
                recipeStep.ingredients
            )
            db.addInstruction(instruction)
        }
    }
    order.estimatedTimeShipping = latestScheduledEndOfProcedure
    return true
}

fun printMachineOccupation(db: DBManager) {
    println("\n### Current Machine Occupation ###")
    db.fetchMachines().forEach {
        println(
            "Machine \"${it.name}\" (ID: ${it.id}) - #${it.procedures.distinct().size} procedures: " +
                    "${if (it.status == MachineStatus.BROKEN) it.status else it.occupiedUntil}"
        )
    }
    println("##################################")
}