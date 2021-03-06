import java.sql.*
import kotlin.random.Random

class DBManager {
    private val verbose: Boolean = false

    private var dbConnection: Connection? = null
    fun openConnection() {
        check(dbConnection == null) { "Already connected to database" }
        try {
            dbConnection = DriverManager.getConnection(DB_URL)
            println("Opened connection.")
        } catch (e: SQLException) {
            throw handleError("Could not open connection.", e)
        }
    }

    fun closeConnection() {
        dbConnection?.run {
            close()
            println("Closed connection.")
        } ?: println("Connection was already closed.")
    }

    fun initializeTables() {
        println("Initializing new data base.")
        try {
            deleteTables()
        } catch (e: SQLException) {
            throw handleError("Could not delete table.", e)
        }
        try {
            createTables()
        } catch (e: SQLException) {
            throw handleError("Could not create table.", e)
        }
    }

    private fun deleteTables() {
        println("Deleting old tables.")
        dbConnection!!.createStatement()
            .use { statement -> statement.execute("DROP TABLE $INSTRUCTIONS_TABLE_NAME") }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute("DROP TABLE $ORDER_PRODUCTS_TABLE_NAME") }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute("DROP TABLE $ORDERS_TABLE_NAME") }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute("DROP TABLE $PRODUCTS_TABLE_NAME") }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute("DROP TABLE $MACHINE_PROCEDURES_TABLE_NAME") }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute("DROP TABLE $MACHINES_TABLE_NAME") }
    }

    private fun createTables() {
        println("Creating new tables.")

        println(" - Products")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE $PRODUCTS_TABLE_NAME ("
                        + "$PRODUCT_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                        + "$PRODUCT_NAME VARCHAR(255), "
                        + "$PRODUCT_WEIGHT INT, "
                        + "$PRODUCT_RECIPE VARCHAR(255), "
                        + "$PRODUCT_PRIORITY INT)",
            )
        }

        println(" - Orders")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE $ORDERS_TABLE_NAME ("
                        + "$ORDER_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                        + "$ORDER_STATUS VARCHAR(255), "
                        + "$ORDER_PRIORITY INT, "
                        + "$ORDER_SHIPPING_TIME TIME) ",
            )
        }

        println(" - Order products")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE $ORDER_PRODUCTS_TABLE_NAME ("
                        + "$ORDER_ID INT, "
                        + "$PRODUCT_ID INT, "
                        + "FOREIGN KEY ($PRODUCT_ID) REFERENCES $PRODUCTS_TABLE_NAME($PRODUCT_ID), "
                        + "FOREIGN KEY ($ORDER_ID) REFERENCES $ORDERS_TABLE_NAME($ORDER_ID))",
            )
        }

        println(" - Machines")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE $MACHINES_TABLE_NAME ("
                        + "$MACHINE_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                        + "$MACHINE_NAME VARCHAR(255), "
                        + "$MACHINE_OCCUPIED_UNTIL TIME, "
                        + "$MACHINE_STATUS VARCHAR(255),"
                        + "$MACHINE_PATH VARCHAR(255),"
                        + "$MACHINE_WARRANTY DATE)",
            )
        }

        println(" - Machine Procedures")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE $MACHINE_PROCEDURES_TABLE_NAME ("
                        + "$MACHINE_ID INT, "
                        + "$MACHINE_PROCEDURE VARCHAR(255), "
                        + "FOREIGN KEY ($MACHINE_ID) REFERENCES $MACHINES_TABLE_NAME($MACHINE_ID)) ",
            )
        }

        println(" - Instructions")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE $INSTRUCTIONS_TABLE_NAME ("
                        + "$INSTRUCTION_ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                        + "$ORDER_ID INT, "
                        + "$PRODUCT_ID INT, "
                        + "$MACHINE_ID INT, "
                        + "$MACHINE_PROCEDURE VARCHAR(255), "
                        + "$INSTRUCTION_INGREDIENTS VARCHAR(255), "
                        + "$INSTRUCTION_DURATION INT, "
                        + "FOREIGN KEY ($PRODUCT_ID) REFERENCES $PRODUCTS_TABLE_NAME($PRODUCT_ID), "
                        + "FOREIGN KEY ($ORDER_ID) REFERENCES $ORDERS_TABLE_NAME($ORDER_ID), "
                        + "FOREIGN KEY ($MACHINE_ID) REFERENCES $MACHINES_TABLE_NAME($MACHINE_ID)) ",
            )
        }
    }

    fun populateTablesWithDummyData() {
        populateProductsWithDummyData()
        populateOrdersWithDummyData()
        populateMachinesWithDummyData()
    }

    private fun populateMachinesWithDummyData() {
        println("Populating Machines with dummy data.")
        Machine.getDummyMachines().forEach { addMachine(it) }
    }

    private fun populateProductsWithDummyData() {
        println("Populating Products with dummy data.")
        Product.getDummyProducts().forEach { addProduct(it) }
    }

    fun populateOrdersWithDummyData(amount: Int = Random.nextInt(1, 5)) {
        if (verbose) println("Populating Orders with dummy data.")
        repeat(amount) { addOrder(Order.getRandomDummyOrder()) }
    }

    fun addProduct(product: Product) {
        println(" - Adding product \"${product.name}\" to database.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $PRODUCTS_TABLE_NAME ($PRODUCT_NAME, $PRODUCT_RECIPE, $PRODUCT_PRIORITY) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            )
            insertStatement.setString(1, product.name)
            insertStatement.setString(2, product.recipe.serialize())
            insertStatement.setInt(3, product.priority)

            val affectedRows = insertStatement.executeUpdate()
            if (affectedRows != 1) {
                throw handleError("Failed to add product ${product.name} to database.")
            }

            val generatedKeys: ResultSet = insertStatement.generatedKeys
            generatedKeys.next()
            product.id = generatedKeys.getInt(1)
        } catch (e: SQLException) {
            throw handleError("Failed to add product ${product.name} to database.", e)
        }
    }

    fun addOrder(order: Order): Order {
        if (verbose) println(" - Adding order to database.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $ORDERS_TABLE_NAME ( $ORDER_STATUS, $ORDER_SHIPPING_TIME ) VALUES(?,?)",
                Statement.RETURN_GENERATED_KEYS
            )
            insertStatement.setString(1, order.status.name)
            insertStatement.setTimestamp(2, order.estimatedTimeShipping)

            val affectedRows = insertStatement.executeUpdate()
            if (affectedRows != 1) {
                throw handleError("Failed to add order to database.")
            }

            // return created order ID (primary key)
            val generatedKeys: ResultSet = insertStatement.generatedKeys
            generatedKeys.next()
            order.id = generatedKeys.getInt(1)

            order.products.forEach { addOrderItem(order.id, it) }
        } catch (e: SQLException) {
            throw handleError("Failed to add order to database.", e)
        }
        if (verbose) println(" - Order has ID ${order.id} and initial priority ${order.priority}.")
        return order
    }

    private fun addOrderItem(orderID: Int, product: Product) {
        if (verbose) println(" -- Adding product \"${product.name}\" to order $orderID.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $ORDER_PRODUCTS_TABLE_NAME ( $ORDER_ID, $PRODUCT_ID ) VALUES(?,?)"
            )

            insertStatement.setInt(1, orderID)
            insertStatement.setInt(2, product.id)

            val affectedRows = insertStatement.executeUpdate()
            if (affectedRows != 1) {
                throw handleError("Failed to add order_item to database.")
            }

        } catch (e: SQLException) {
            throw handleError("Failed to add order_item to database.", e)
        }
    }

    fun addMachine(machine: Machine) {
        println(" - Adding machine to database.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $MACHINES_TABLE_NAME ( $MACHINE_NAME, $MACHINE_OCCUPIED_UNTIL, $MACHINE_STATUS ) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            )

            insertStatement.setString(1, machine.name)
            insertStatement.setTime(2, Time(machine.occupiedUntil.time))
            insertStatement.setString(3, machine.status.name)

            val affectedRows = insertStatement.executeUpdate()
            if (affectedRows != 1) {
                throw handleError("Failed to add order to database.")
            }

            // return created order ID (primary key)
            val generatedKeys: ResultSet = insertStatement.generatedKeys
            generatedKeys.next()
            machine.id = generatedKeys.getInt(1)

            machine.procedures.forEach { addMachineProcedure(machine.id, it) }
        } catch (e: SQLException) {
            throw handleError("Failed to add order to database.", e)
        }
        println(" - Machine ${machine.name} has ID ${machine.id} and is ${machine.status}.")
    }

    private fun addMachineProcedure(machineID: Int, procedure: Procedure) {
        println(" -- Adding procedure \"${procedure.name}\" to machine $machineID.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $MACHINE_PROCEDURES_TABLE_NAME ( $MACHINE_ID, $MACHINE_PROCEDURE ) VALUES(?,?)"
            )

            insertStatement.setInt(1, machineID)
            insertStatement.setString(2, procedure.name)

            val affectedRows = insertStatement.executeUpdate()
            if (affectedRows != 1) {
                throw handleError("Failed to add order_item to database.")
            }

        } catch (e: SQLException) {
            throw handleError("Failed to add order_item to database.", e)
        }
    }

    fun addInstruction(instruction: Instruction) {
        println(" -- Storing instruction ${instruction.procedure} ${instruction.ingredients} for ${instruction.machine.name} (ID: ${instruction.machine.id}).")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $INSTRUCTIONS_TABLE_NAME ( $ORDER_ID, $PRODUCT_ID, $MACHINE_ID, $MACHINE_PROCEDURE, " +
                        "$INSTRUCTION_INGREDIENTS, $INSTRUCTION_DURATION ) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            )

            insertStatement.setInt(1, instruction.order.id)
            insertStatement.setInt(2, instruction.product.id)
            insertStatement.setInt(3, instruction.machine.id)
            insertStatement.setString(4, instruction.procedure.name)
            insertStatement.setString(5, Ingredient.serialize(instruction.ingredients))
            insertStatement.setInt(6, instruction.durationInSec)

            val affectedRows = insertStatement.executeUpdate()
            if (affectedRows != 1) {
                throw handleError("Failed to add instruction to database.")
            }

            val generatedKeys: ResultSet = insertStatement.generatedKeys
            generatedKeys.next()
            instruction.id = generatedKeys.getInt(1)

        } catch (e: SQLException) {
            throw handleError("Failed to add instruction to database.", e)
        }
    }

    /**
     * _Should_ normally be procedurally triggered when an order is inserted, is called whenever an order is fetched instead.
     * Looks for new orders in database put there by Webshop (marked by status PAID).
     * Calculates their initial priority.
     * Updates their status from PAID to WAITING.
     * */
    private fun calculateInitialPriority() {
        try {
            val selectStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "SELECT $ORDERS_TABLE_NAME.$ORDER_ID, $PRODUCTS_TABLE_NAME.$PRODUCT_PRIORITY " +
                        "FROM $ORDERS_TABLE_NAME, $ORDER_PRODUCTS_TABLE_NAME, $PRODUCTS_TABLE_NAME " +
                        "WHERE $ORDERS_TABLE_NAME.$ORDER_ID = $ORDER_PRODUCTS_TABLE_NAME.$ORDER_ID AND " +
                        "$ORDER_PRODUCTS_TABLE_NAME.$PRODUCT_ID = $PRODUCTS_TABLE_NAME.$PRODUCT_ID AND " +
                        "$ORDERS_TABLE_NAME.$ORDER_STATUS = ?"
            )
            selectStatement.setString(1, OrderStatus.PAID.name)

            val resultSet = selectStatement.executeQuery()
            var prevId = -1
            var orderPriority = 0
            while (resultSet.next()) {
                if (prevId == -1) println("\nFound new order(s):")
                val orderId = resultSet.getInt(1)
                if (prevId != -1 && (prevId != orderId)) {
                    updatePriority(prevId, orderPriority)
                    orderPriority = 0
                }
                orderPriority += resultSet.getInt(2) // actual calculation of priority
                // to keep this part a bit more lightweight, I decided against creating the whole order object to use its priority calculation
                prevId = orderId
            }
            if (prevId != -1) updatePriority(prevId, orderPriority)

        } catch (e: SQLException) {
            throw handleError("Order priority could not be updated.", e)
        }

    }

    private fun updatePriority(orderId: Int, orderPriority: Int) {
        val updatePriorityStatement: PreparedStatement = dbConnection!!.prepareStatement(
            "UPDATE $ORDERS_TABLE_NAME SET $ORDER_PRIORITY = ? WHERE $ORDER_ID = ?"
        )
        updatePriorityStatement.setInt(1, orderPriority)
        updatePriorityStatement.setInt(2, orderId)
        updatePriorityStatement.executeUpdate()

        val updateStatusStatement: PreparedStatement = dbConnection!!.prepareStatement(
            "UPDATE $ORDERS_TABLE_NAME SET $ORDER_STATUS = ? WHERE $ORDER_ID = ?"
        )
        updateStatusStatement.setString(1, OrderStatus.WAITING.name)
        updateStatusStatement.setInt(2, orderId)
        updateStatusStatement.executeUpdate()
        println(" - Found order with ID: ${orderId}, assigned it initial priority: $orderPriority).")
    }

    /**
     * Fetches the next order with the highest priority and status WAITING.
     */
    fun fetchNextOrder(): Order? {
        try {
            calculateInitialPriority() // ensures that all orders have right priority

            val selectStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "SELECT $ORDERS_TABLE_NAME.$ORDER_ID, $ORDERS_TABLE_NAME.$ORDER_PRIORITY, " +
                        "$PRODUCTS_TABLE_NAME.$PRODUCT_ID, $PRODUCTS_TABLE_NAME.$PRODUCT_NAME, " +
                        "$PRODUCTS_TABLE_NAME.$PRODUCT_RECIPE, $PRODUCTS_TABLE_NAME.$PRODUCT_PRIORITY " +
                        "FROM $ORDERS_TABLE_NAME, $ORDER_PRODUCTS_TABLE_NAME, $PRODUCTS_TABLE_NAME " +
                        "WHERE $ORDERS_TABLE_NAME.$ORDER_ID = $ORDER_PRODUCTS_TABLE_NAME.$ORDER_ID AND " +
                        "$ORDER_PRODUCTS_TABLE_NAME.$PRODUCT_ID = $PRODUCTS_TABLE_NAME.$PRODUCT_ID AND " +
                        "$ORDERS_TABLE_NAME.$ORDER_STATUS = ?" +
                        "ORDER BY $ORDERS_TABLE_NAME.$ORDER_PRIORITY DESC"
            )
            selectStatement.setString(1, OrderStatus.WAITING.name)

            val resultSet = selectStatement.executeQuery()
            var prevId = -1
            var order: Order? = null
            val products = mutableListOf<Product>()
            var newOrder = true
            while (resultSet.next()) {
                val orderId = resultSet.getInt(1)
                if (prevId != -1 && prevId != orderId) break
                if (newOrder) {
                    val orderPriority = resultSet.getInt(2)
                    order = Order(orderId, products)
                    order.priority = orderPriority
                    newOrder = false
                }
                val productID = resultSet.getInt(3)
                val productName = resultSet.getString(4)
                val productRecipe = resultSet.getString(5)
                val productPriority = resultSet.getInt(6)
                products.add(Product(productID, productName, Recipe.deserialize(productRecipe), productPriority))
                prevId = orderId
            }
            order?.calculateMinimumTimeOfShipping()
            return order

        } catch (e: SQLException) {
            throw handleError("Failed to fetch order from database.", e)
        }
    }

    /**
     * Fetches all WORKing machines implementing a procedure, sorted by the earliest time they can receive new instructions.
     */
    fun fetchMachine(procedure: Procedure): Machine? {
        //println(" -- Fetching machine for procedure \"${procedure.name}\"")
        try {
            val selectStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "SELECT $MACHINES_TABLE_NAME.$MACHINE_ID, $MACHINES_TABLE_NAME.$MACHINE_NAME, " +
                        "$MACHINE_PROCEDURES_TABLE_NAME.$MACHINE_PROCEDURE, $MACHINES_TABLE_NAME.$MACHINE_OCCUPIED_UNTIL " +
                        "FROM $MACHINES_TABLE_NAME, $MACHINE_PROCEDURES_TABLE_NAME " +
                        "WHERE $MACHINES_TABLE_NAME.$MACHINE_ID = $MACHINE_PROCEDURES_TABLE_NAME.$MACHINE_ID AND " +
                        "$MACHINES_TABLE_NAME.$MACHINE_STATUS = ? AND " +
                        "$MACHINE_PROCEDURES_TABLE_NAME.$MACHINE_PROCEDURE = ? " +
                        "ORDER BY $MACHINES_TABLE_NAME.$MACHINE_OCCUPIED_UNTIL ASC"
            )
            selectStatement.setString(1, MachineStatus.WORKING.name)
            selectStatement.setString(2, procedure.name)

            val resultSet = selectStatement.executeQuery()
            var prevId = -1
            var machine: Machine? = null
            val procedures = mutableListOf<Procedure>()
            while (resultSet.next()) {
                val machineId = resultSet.getInt(1)
                if (prevId != -1 && prevId != machineId) break
                val machineName = resultSet.getString(2)
                val procedureString = resultSet.getString(3)
                procedures.add(Procedure.valueOf(procedureString))
                val occupiedUntil = resultSet.getTimestamp(4)
                machine = Machine(machineId, machineName, procedures, occupiedUntil)
                prevId = machineId
            }
            return machine

        } catch (e: SQLException) {
            throw handleError("Failed to fetch machine for procedure.", e)
        }
    }

    fun updateOrderStatus(orderID: Int, orderStatus: OrderStatus = OrderStatus.SCHEDULED) {
        try {
            val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "UPDATE $ORDERS_TABLE_NAME SET $ORDER_STATUS = ? WHERE $ORDER_ID = ?"
            )
            updateStatement.setString(1, orderStatus.name)
            updateStatement.setInt(2, orderID)
            updateStatement.executeUpdate()
        } catch (e: SQLException) {
            throw handleError("Order status could not be updated.", e)
        }
    }

    fun fetchMachines(): List<Machine> {
        try {
            val selectStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "SELECT $MACHINES_TABLE_NAME.$MACHINE_ID, $MACHINES_TABLE_NAME.$MACHINE_NAME, $MACHINES_TABLE_NAME.$MACHINE_OCCUPIED_UNTIL," +
                        "$MACHINES_TABLE_NAME.$MACHINE_STATUS, $MACHINE_PROCEDURES_TABLE_NAME.$MACHINE_PROCEDURE " +
                        "FROM $MACHINES_TABLE_NAME, $MACHINE_PROCEDURES_TABLE_NAME " +
                        "WHERE $MACHINES_TABLE_NAME.$MACHINE_ID = $MACHINE_PROCEDURES_TABLE_NAME.$MACHINE_ID " +
                        "ORDER BY $MACHINES_TABLE_NAME.$MACHINE_OCCUPIED_UNTIL DESC"
            )

            val resultSet = selectStatement.executeQuery()
            var prevId = -1
            val machines = mutableListOf<Machine>()
            var procedures = mutableListOf<Procedure>()
            var machineId: Int

            while (resultSet.next()) {
                machineId = resultSet.getInt(1)
                if (prevId != machineId) procedures = mutableListOf()

                val machineName = resultSet.getString(2)
                val occupiedUntil = resultSet.getTimestamp(3)
                val status = MachineStatus.valueOf(resultSet.getString(4))
                val procedureString = resultSet.getString(5)
                procedures.add(Procedure.valueOf(procedureString))

                if (prevId != machineId) {
                    machines.add(Machine(machineId, machineName, procedures, occupiedUntil, status))
                }
                prevId = machineId
            }
            return machines

        } catch (e: SQLException) {
            throw handleError("Failed to fetch machine for procedure.", e)
        }
    }

    fun increaseOrderPriority(amount: Int = 1) {
        try {
            val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "UPDATE $ORDERS_TABLE_NAME SET $ORDER_PRIORITY = $ORDER_PRIORITY + ? WHERE $ORDER_STATUS = ?"
            )
            updateStatement.setInt(1, amount)
            updateStatement.setString(2, OrderStatus.WAITING.name)
            updateStatement.executeUpdate()
        } catch (e: SQLException) {
            throw handleError("Order status could not be updated.", e)
        }
    }

    fun updateMachineOccupation(machineId: Int, timestamp: Timestamp) {
        try {
            val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "UPDATE $MACHINES_TABLE_NAME SET $MACHINE_OCCUPIED_UNTIL = ? WHERE $MACHINE_ID = ?"
            )
            updateStatement.setTimestamp(1, timestamp)
            updateStatement.setInt(2, machineId)
            updateStatement.executeUpdate()
        } catch (e: SQLException) {
            throw handleError("Machine occupation could not be updated.", e)
        }
    }

    fun updateEstimatedShippingTime(orderID: Int, shippingTime: Timestamp) {
        try {
            val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "UPDATE $ORDERS_TABLE_NAME SET $ORDER_SHIPPING_TIME = ? WHERE $ORDER_ID = ?"
            )
            updateStatement.setTimestamp(1, shippingTime)
            updateStatement.setInt(2, orderID)
            updateStatement.executeUpdate()
        } catch (e: SQLException) {
            throw handleError("Shipping time could not be updated.", e)
        }
    }


    fun startTransaction() {
        dbConnection!!.autoCommit = false
    }

    fun commitTransaction() {
        dbConnection!!.commit()
        dbConnection!!.autoCommit = true
    }

    fun rollbackTransaction() {
        dbConnection!!.rollback()
        dbConnection!!.autoCommit = true
    }

    /**
     * Makes sure that a connection is closed if a fatal error occurs.
     */
    private fun handleError(message: String, cause: Exception? = null): RuntimeException {
        try {
            closeConnection()
        } catch (e: SQLException) {
            throw RuntimeException("Could not close connection.", e)
        }
        return if (cause != null) RuntimeException(cause) else RuntimeException(message)
    }

    companion object {
        private const val DB_URL = "jdbc:derby:demoDB;create=true"

        // table names and their columns
        private const val ORDERS_TABLE_NAME = "ORDERS"
        private const val ORDER_ID = "ORDER_ID"
        private const val ORDER_STATUS = "STATUS"
        private const val ORDER_PRIORITY = "PRIORITY"
        private const val ORDER_SHIPPING_TIME = "ESTIMATED_SHIPPING_TIME"

        private const val PRODUCTS_TABLE_NAME = "PRODUCTS"
        private const val PRODUCT_ID = "PRODUCT_ID"
        private const val PRODUCT_NAME = "PRODUCT_NAME"
        private const val PRODUCT_WEIGHT = "WEIGHT" // needed for webshop
        private const val PRODUCT_RECIPE = "RECIPE"
        private const val PRODUCT_PRIORITY = "PRIORITY"

        private const val ORDER_PRODUCTS_TABLE_NAME = "ORDER_PRODUCTS"

        private const val MACHINES_TABLE_NAME = "MACHINES"
        private const val MACHINE_ID = "MACHINE_ID"
        private const val MACHINE_NAME = "NAME"
        private const val MACHINE_OCCUPIED_UNTIL = "OCCUPIED_UNTIL"
        private const val MACHINE_STATUS = "STATUS"
        private const val MACHINE_PATH = "ACCESS_PATH" // needed for shopfloor
        private const val MACHINE_WARRANTY = "WARRANTY" // needed for shopfloor

        private const val MACHINE_PROCEDURES_TABLE_NAME = "MACHINE_PROCEDURES"
        private const val MACHINE_PROCEDURE = "MACHINE_PROCEDURE"

        private const val INSTRUCTIONS_TABLE_NAME = "INSTRUCTIONS"
        private const val INSTRUCTION_ID = "INSTRUCTION_ID"
        private const val INSTRUCTION_INGREDIENTS = "INGREDIENTS"
        private const val INSTRUCTION_DURATION = "DURATION"
    }
}