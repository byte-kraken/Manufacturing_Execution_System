import Converter.Companion.secondsToMillis
import java.sql.SQLException
import java.sql.Timestamp

/** Demo of the Scheduler using dummy data */
fun main() {
    val db = DBManager()
    db.openConnection()
    try {
        db.initializeTables()
        db.populateTablesWithDummyData()

        println("\n---------- Starting MES ----------")
        while (true) {
            val order = db.fetchNextOrder()
            if (order != null) {
                Scheduler.handleOrder(order, db)
                Thread.sleep(1000)
            } else {
                Scheduler.printMachineOccupation(db)
                Thread.sleep(6000)
                db.populateOrdersWithDummyData()
            }
        }
    } catch (e: SQLException) {
        e.printStackTrace()
    } finally {
        db.closeConnection()
    }
}

class Scheduler {
    companion object {
        /** Schedules order, updates its status, increases priority of all other orders */
        fun handleOrder(order: Order, db: DBManager) {
            if (schedule(order, db)) {
                db.updateOrderStatus(order.id, OrderStatus.SCHEDULED)
                println(
                    "Order ${order.id} was scheduled successfully. " +
                            "Estimated time of shipping: ${order.estimatedTimeShipping}"
                )
            } else {
                println("Order ${order.id} was NOT scheduled successfully.")
                db.updateOrderStatus(order.id, OrderStatus.NOT_DELIVERABLE)
            }
            db.increaseOrderPriority()
        }

        /**
         * Analyzes the recipe of all products in an order.
         * For each procedure in these recipes, out of all machines capable of executing it, selects the least occupied one.
         * Assigns the procedure to the machine by creating and storing an instructions.
         * Calculates estimated time the order can be shipped based on assignments.
         * Updates machine occupation along the way.
         *
         * @return false, if an order could not be scheduled (e.g. no machine was capable of executing a procedure)
         */
        fun schedule(order: Order, db: DBManager): Boolean {
            println("\nScheduling order ${order.id} (Prio: ${order.priority}) with the following products: ${order.products.map { it.name + " (Prio: " + it.priority + ")" }}")

            // tracks the time the last procedure is completed = the order is completed
            var latestScheduledEndOfProcedure = Timestamp(0)
            // tracks the instructions needed to fulfill the order
            val instructions: MutableList<Instruction> = mutableListOf()

            // generating instructions for machines
            order.products.forEach { product ->

                println(" - Analyzing recipe for ${product.name}")
                db.startTransaction()
                product.recipe.steps.forEach { recipeStep ->
                    val machine = db.fetchMachine(recipeStep.procedure) ?: run {
                        println(" ## No working machine for ${recipeStep.procedure} in ${product.name} was found, cancelling order.")
                        db.rollbackTransaction()
                        return false
                    }

                    // calculating and setting new machine occupation
                    val laterStartOfNewProcedure = maxOf(machine.occupiedUntil.time, System.currentTimeMillis())
                    machine.occupiedUntil =
                        Timestamp(laterStartOfNewProcedure + recipeStep.durationInSec.secondsToMillis())
                    db.updateMachineOccupation(machine.id, machine.occupiedUntil)

                    // calculating new earliest time the order can be finished
                    latestScheduledEndOfProcedure =
                        Timestamp(maxOf(latestScheduledEndOfProcedure.time, machine.occupiedUntil.time))

                    // creating new instruction
                    val instruction = Instruction(
                        -1,
                        order,
                        product,
                        machine,
                        recipeStep.procedure,
                        recipeStep.ingredients,
                        recipeStep.durationInSec
                    )
                    instructions.add(instruction)
                }
            }
            db.commitTransaction()

            order.estimatedTimeShipping = latestScheduledEndOfProcedure

            // writing instructions to database
            println(" - All procedures can be executed")
            instructions.forEach { db.addInstruction(it) }
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
    }
}