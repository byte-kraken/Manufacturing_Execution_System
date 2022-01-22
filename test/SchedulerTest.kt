import Converter.Companion.millisToSeconds
import Converter.Companion.secondsToMillis
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class SchedulerTest {
    @BeforeEach
    fun setUp() {
        db.initializeTables()
        println("\n----------------\n")
    }

    @Test
            /** checks if scheduler balances load between machines */
    fun scheduleEffectively() {
        // two equal machines are added to database
        db.addMachine(workingPointlessMachine)
        val prevTimeMachine1 =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachine.id }[0].occupiedUntil
        db.addMachine(workingPointlessMachineCopy)
        val prevTimeMachine2 =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachineCopy.id }[0].occupiedUntil

        // control machine not implementing required procedure
        db.addMachine(workingAmazingMachine)
        val prevTimeMachineControl =
            db.fetchMachines().filter { machine -> machine.id == workingAmazingMachine.id }[0].occupiedUntil

        // two equal orders are given, each with one procedure
        db.addProduct(simpleProduct)
        db.addOrder(simpleOrder)
        db.addOrder(simpleOrderCopy)

        // both machines should be used once
        val success1 = Scheduler.schedule(simpleOrder, db)
        assertTrue(success1)
        val success2 = Scheduler.schedule(simpleOrderCopy, db)
        assertTrue(success2)

        // both should have an increased occupation time
        val newTimeMachine1 =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachine.id }[0].occupiedUntil
        assertNotEquals(prevTimeMachine1, newTimeMachine1)
        val newTimeMachine2 =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachineCopy.id }[0].occupiedUntil
        assertNotEquals(prevTimeMachine2, newTimeMachine2)

        // control should not have changed
        val newTimeMachineControl =
            db.fetchMachines().filter { machine -> machine.id == workingAmazingMachine.id }[0].occupiedUntil
        assertEquals(prevTimeMachineControl, newTimeMachineControl)
    }

    @Test
            /** checks if scheduler balances load between machines */
    fun correctShippingTimeCalculated() {
        db.addMachine(workingPointlessMachine)
        db.addProduct(simpleProduct)
        db.addOrder(simpleOrder)
        db.addOrder(simpleOrderCopy)

        Scheduler.schedule(simpleOrder, db)
        val order1ShippingTime = simpleOrder.estimatedTimeShipping
        Scheduler.schedule(simpleOrderCopy, db)
        val order2ShippingTime = simpleOrderCopy.estimatedTimeShipping
        val timeDifference = order2ShippingTime.time - order1ShippingTime.time
        // timeDifference should be the time the procedure takes to execute

        // accounting for rounding errors in database
        assertTrue((simpleProduct.recipe.steps[0].durationInSec.secondsToMillis() - timeDifference).millisToSeconds() in -1..1)
    }

    @Test
            /** orders should be fetched based on priority, which should be higher for complex orders */
    fun fetchCorrectInitialPrio() {
        db.addProduct(simpleProduct)
        db.addProduct(simpleProduct2)
        db.addOrder(simpleOrder)
        db.addOrder(hardOrder)
        db.addOrder(mediumOrder)

        var order = db.fetchNextOrder()
        assertEquals(hardOrder.id, order!!.id)
        db.updateOrderStatus(order.id)

        order = db.fetchNextOrder()
        assertEquals(mediumOrder.id, order!!.id)
        db.updateOrderStatus(order.id)

        order = db.fetchNextOrder()
        assertEquals(simpleOrder.id, order!!.id)
    }

    @Test
            /** priorities should increase over time */
    fun prioIncreases() {
        db.addProduct(simpleProduct)

        // all orders start with the same initial prio
        db.addOrder(simpleOrder)
        db.addOrder(simpleOrder)
        db.addOrder(simpleOrder)

        var order = db.fetchNextOrder()
        val initPrio = order!!.priority
        Scheduler.handleOrder(order!!, db)

        order = db.fetchNextOrder()
        val incrPrio1 = order!!.priority
        Scheduler.handleOrder(order!!, db)
        assertEquals(initPrio + 1, incrPrio1)

        order = db.fetchNextOrder()
        val incrPrio2 = order!!.priority
        assertEquals(incrPrio1 + 1, incrPrio2)
    }

    @Test
            /** broken machine should not be used */
    fun brokenMachineUsed() {
        // broken machine with required procedure
        db.addMachine(brokenPointlessMachine)
        db.addProduct(simpleProduct)
        db.addOrder(simpleOrder)
        var success = Scheduler.schedule(simpleOrder, db)
        assertFalse(success)


        // working machine with same specs
        db.addMachine(workingPointlessMachine)
        db.addOrder(simpleOrder)
        val prevTime =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachine.id }[0].occupiedUntil
        success = Scheduler.schedule(simpleOrder, db)
        assertTrue(success)

        val newTime = db.fetchMachines().filter { machine -> machine.id == workingPointlessMachine.id }[0].occupiedUntil
        assertNotEquals(prevTime, newTime)

    }

    @Test
            /** occupation for successful products in broken order should be reset */
    fun orderImpossibleRollbackRequired() {
        // test orders with those products and procedures using machines
        db.addProduct(simpleProduct)
        db.addMachine(workingPointlessMachine)
        val initialTime =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachine.id }[0].occupiedUntil

        db.addOrder(simpleOrder) // procedure implemented
        var success = Scheduler.schedule(simpleOrder, db)
        assertTrue(success)
        val simpleOrderTime =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachine.id }[0].occupiedUntil
        assertNotEquals(initialTime, simpleOrderTime)

        db.addProduct(simpleProduct2)
        db.addOrder(mediumOrder) // procedure partially not implemented
        success = Scheduler.schedule(mediumOrder, db)
        assertFalse(success)

        // ensure rollback took place
        val mediumOrderTime =
            db.fetchMachines().filter { machine -> machine.id == workingPointlessMachine.id }[0].occupiedUntil
        assertEquals(simpleOrderTime, mediumOrderTime)
    }

    companion object {
        val db = DBManager()

        @BeforeAll
        @JvmStatic
        fun initialize() {
            db.openConnection()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            db.closeConnection()
        }
    }
}