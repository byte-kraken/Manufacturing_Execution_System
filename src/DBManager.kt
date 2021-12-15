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
        if (dbConnection == null) {
            println("Connection was already closed.")
        } else {
            dbConnection!!.close()
            println("Closed connection.")
        }
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
            .use { statement -> statement.execute(String.format("DROP TABLE %s", INSTRUCTIONS_TABLE_NAME)) }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute(String.format("DROP TABLE %s", ORDER_ITEMS_TABLE_NAME)) }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute(String.format("DROP TABLE %s", ORDERS_TABLE_NAME)) }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute(String.format("DROP TABLE %s", PRODUCTS_TABLE_NAME)) }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute(String.format("DROP TABLE %s", MACHINE_PROCEDURES_TABLE_NAME)) }
        dbConnection!!.createStatement()
            .use { statement -> statement.execute(String.format("DROP TABLE %s", MACHINES_TABLE_NAME)) }
    }

    private fun createTables() {
        println("Creating new tables.")

        println(" - Products")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                String.format(
                    "CREATE TABLE %s ("
                            + "%s VARCHAR(255) PRIMARY KEY,"
                            + "%s VARCHAR(255), "
                            + "%s INT)",
                    PRODUCTS_TABLE_NAME, PRODUCT_NAME, PRODUCT_RECIPE, PRODUCT_PRIORITY
                )
            )
        }

        println(" - Orders")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                String.format(
                    "CREATE TABLE %s ("
                            + "%s INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                            + "%s VARCHAR(255), "
                            + "%s INT) ",
                    ORDERS_TABLE_NAME, ORDER_ID, ORDER_STATUS, ORDER_PRIORITY
                )
            )
        }

        println(" - Order items")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                String.format(
                    "CREATE TABLE %s ("
                            + "%s INT, "
                            + "%s VARCHAR(255), "
                            + "FOREIGN KEY ($PRODUCT_NAME) REFERENCES $PRODUCTS_TABLE_NAME($PRODUCT_NAME), "
                            + "FOREIGN KEY ($ORDER_ID) REFERENCES $ORDERS_TABLE_NAME($ORDER_ID))",
                    ORDER_ITEMS_TABLE_NAME, ORDER_ID, PRODUCT_NAME
                )
            )
        }

        println(" - Machines")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                String.format(
                    "CREATE TABLE %s ("
                            + "%s INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                            + "%s VARCHAR(255), "
                            + "%s TIME, "
                            + "%s VARCHAR(255))",
                    MACHINES_TABLE_NAME, MACHINE_ID, MACHINE_NAME, MACHINE_OCCUPIED_UNTIL, MACHINE_STATUS
                )
            )
        }

        println(" - Machine Procedures")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                String.format(
                    "CREATE TABLE %s ("
                            + "%s INT, "
                            + "%s VARCHAR(255), "
                            + "FOREIGN KEY ($MACHINE_ID) REFERENCES $MACHINES_TABLE_NAME($MACHINE_ID)) ",
                    MACHINE_PROCEDURES_TABLE_NAME, MACHINE_ID, MACHINE_PROCEDURE
                )
            )
        }

        println(" - Instructions")
        dbConnection!!.createStatement().use { statement ->
            statement.execute(
                String.format(
                    "CREATE TABLE %s ("
                            + "%s INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                            + "%s INT, "
                            + "%s VARCHAR(255), "
                            + "%s INT, "
                            + "%s VARCHAR(255), "
                            + "%s VARCHAR(255), "
                            + "%s INT) ",
                    INSTRUCTIONS_TABLE_NAME,
                    INSTRUCTION_ID,
                    ORDER_ID,
                    PRODUCT_NAME,
                    MACHINE_ID,
                    MACHINE_PROCEDURE,
                    INSTRUCTION_INGREDIENTS,
                    INSTRUCTION_DURATION
                )
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
                "INSERT INTO $PRODUCTS_TABLE_NAME ($PRODUCT_NAME, $PRODUCT_RECIPE, $PRODUCT_PRIORITY) VALUES(?,?,?)"
            )
            insertStatement.setString(1, product.name)
            insertStatement.setString(2, product.recipe.serialize())
            insertStatement.setInt(3, product.priority)

            val affectedRows = insertStatement.executeUpdate();
            if (affectedRows != 1) {
                throw handleError("Failed to add product ${product.name} to database.");
            }
        } catch (e: SQLException) {
            throw handleError("Failed to add product ${product.name} to database.", e);
        }
    }

    fun addOrder(order: Order) {
        if (verbose) println(" - Adding order to database.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $ORDERS_TABLE_NAME ( $ORDER_STATUS, $ORDER_PRIORITY ) VALUES(?,?)",
                Statement.RETURN_GENERATED_KEYS
            )

            insertStatement.setString(1, order.status.name)
            insertStatement.setInt(2, order.priority)

            val affectedRows = insertStatement.executeUpdate();
            if (affectedRows != 1) {
                throw handleError("Failed to add order to database.");
            }

            // return created order ID (primary key)
            val generatedKeys: ResultSet = insertStatement.generatedKeys
            generatedKeys.next()
            order.id = generatedKeys.getInt(1)

            order.products.forEach { addOrderItem(order.id, it) }
        } catch (e: SQLException) {
            throw handleError("Failed to add order to database.", e);
        }
        if (verbose) println(" - Order has ID ${order.id} and initial priority ${order.priority}.")
    }

    private fun addOrderItem(orderID: Int, product: Product) {
        if (verbose) println(" -- Adding product \"${product.name}\" to order $orderID.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $ORDER_ITEMS_TABLE_NAME ( $ORDER_ID, $PRODUCT_NAME ) VALUES(?,?)"
            )

            insertStatement.setInt(1, orderID)
            insertStatement.setString(2, product.name)

            val affectedRows = insertStatement.executeUpdate();
            if (affectedRows != 1) {
                throw handleError("Failed to add order_item to database.");
            }

        } catch (e: SQLException) {
            throw handleError("Failed to add order_item to database.", e);
        }
    }

    private fun addMachine(machine: Machine) {
        println(" - Adding machine to database.")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $MACHINES_TABLE_NAME ( $MACHINE_NAME, $MACHINE_OCCUPIED_UNTIL, $MACHINE_STATUS ) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            )

            insertStatement.setString(1, machine.name)
            insertStatement.setTime(2, Time(machine.occupiedUntil.time))
            insertStatement.setString(3, machine.status.name)

            val affectedRows = insertStatement.executeUpdate();
            if (affectedRows != 1) {
                throw handleError("Failed to add order to database.");
            }

            // return created order ID (primary key)
            val generatedKeys: ResultSet = insertStatement.generatedKeys
            generatedKeys.next()
            machine.id = generatedKeys.getInt(1)

            machine.procedures.forEach { addMachineProcedure(machine.id, it) }
        } catch (e: SQLException) {
            throw handleError("Failed to add order to database.", e);
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

            val affectedRows = insertStatement.executeUpdate();
            if (affectedRows != 1) {
                throw handleError("Failed to add order_item to database.");
            }

        } catch (e: SQLException) {
            throw handleError("Failed to add order_item to database.", e);
        }
    }

    fun addInstruction(instruction: Instruction) {
        println(" -- Storing instruction ${instruction.procedure} ${instruction.ingredients} for ${instruction.machine.name} (ID: ${instruction.machine.id}).")
        try {
            val insertStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "INSERT INTO $INSTRUCTIONS_TABLE_NAME ( $ORDER_ID, $PRODUCT_NAME, $MACHINE_ID, $MACHINE_PROCEDURE, " +
                        "$INSTRUCTION_INGREDIENTS, $INSTRUCTION_DURATION ) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            )

            insertStatement.setInt(1, instruction.order.id)
            insertStatement.setString(2, instruction.product.name)
            insertStatement.setInt(3, instruction.machine.id)
            insertStatement.setString(4, instruction.procedure.name)
            insertStatement.setString(5, Ingredient.serialize(instruction.ingredients))
            insertStatement.setInt(6, instruction.product.recipe.getDurationInSec())

            val affectedRows = insertStatement.executeUpdate();
            if (affectedRows != 1) {
                throw handleError("Failed to add instruction to database.");
            }

            val generatedKeys: ResultSet = insertStatement.generatedKeys
            generatedKeys.next()
            instruction.id = generatedKeys.getInt(1)

        } catch (e: SQLException) {
            throw handleError("Failed to add instruction to database.", e);
        }
    }

    /**
     * Fetches the next order with the highest priority and status WAITING.
     */
    fun fetchNextOrder(): Order? {
        try {
            val selectStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "SELECT $ORDERS_TABLE_NAME.$ORDER_ID, $ORDERS_TABLE_NAME.$ORDER_PRIORITY, " +
                        "$PRODUCTS_TABLE_NAME.$PRODUCT_NAME, $PRODUCTS_TABLE_NAME.$PRODUCT_RECIPE, " +
                        "$PRODUCTS_TABLE_NAME.$PRODUCT_PRIORITY " +
                        "FROM $ORDERS_TABLE_NAME, $ORDER_ITEMS_TABLE_NAME, $PRODUCTS_TABLE_NAME " +
                        "WHERE $ORDERS_TABLE_NAME.$ORDER_ID = $ORDER_ITEMS_TABLE_NAME.$ORDER_ID AND " +
                        "$ORDER_ITEMS_TABLE_NAME.$PRODUCT_NAME = $PRODUCTS_TABLE_NAME.$PRODUCT_NAME AND " +
                        "$ORDERS_TABLE_NAME.$ORDER_STATUS = ?" +
                        "ORDER BY $ORDERS_TABLE_NAME.$ORDER_PRIORITY DESC"
            )
            selectStatement.setString(1, OrderStatus.WAITING.name)

            val resultSet = selectStatement.executeQuery();
            var prevId = -1
            var order: Order? = null
            val products = mutableListOf<Product>()
            while (resultSet.next()) {
                val orderId = resultSet.getInt(1)
                if (prevId != -1 && prevId != orderId) break
                val orderPriority = resultSet.getInt(2)
                order = Order(orderId, products)
                order.priority = orderPriority
                val productName = resultSet.getString(3)
                val productRecipe = resultSet.getString(4)
                val productPriority = resultSet.getInt(5)
                products.add(Product(productName, Recipe.deserialize(productRecipe), productPriority))
                prevId = orderId
            }
            return order

        } catch (e: SQLException) {
            throw handleError("Failed to fetch order from database.", e);
        }
    }

    /**
     * Fetches all working machines implementing a procedure, sorted by the earliest time they can receive new instructions.
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

            val resultSet = selectStatement.executeQuery();
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
            throw handleError("Failed to fetch machine for procedure.", e);
        }
    }

    fun updateOrderStatus(orderID: Int, orderStatus: OrderStatus = OrderStatus.SCHEDULED) {
        try {
            val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "UPDATE $ORDERS_TABLE_NAME SET $ORDER_STATUS = ? WHERE $ORDER_ID = ?"
            )
            updateStatement.setString(1, orderStatus.name);
            updateStatement.setInt(2, orderID);
            updateStatement.executeUpdate();
        } catch (e: SQLException) {
            throw handleError("Order status could not be updated.", e);
        }
    }

    /**
     * Provides information on the current scheduling efficiency by returning each machines occupation.
     */
    fun fetchMachines(): List<Machine> {
        try {
            val selectStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "SELECT $MACHINES_TABLE_NAME.$MACHINE_ID, $MACHINES_TABLE_NAME.$MACHINE_NAME, $MACHINES_TABLE_NAME.$MACHINE_OCCUPIED_UNTIL," +
                        "$MACHINES_TABLE_NAME.$MACHINE_STATUS, $MACHINE_PROCEDURES_TABLE_NAME.$MACHINE_PROCEDURE " +
                        "FROM $MACHINES_TABLE_NAME, $MACHINE_PROCEDURES_TABLE_NAME " +
                        "WHERE $MACHINES_TABLE_NAME.$MACHINE_ID = $MACHINE_PROCEDURES_TABLE_NAME.$MACHINE_ID " +
                        "ORDER BY $MACHINES_TABLE_NAME.$MACHINE_OCCUPIED_UNTIL DESC"
            )

            val resultSet = selectStatement.executeQuery();
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
            throw handleError("Failed to fetch machine for procedure.", e);
        }
    }

    fun increaseOrderPriority(amount: Int = 1) {
        try {
            val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "UPDATE $ORDERS_TABLE_NAME SET $ORDER_PRIORITY = $ORDER_PRIORITY + ? WHERE $ORDER_STATUS = ?"
            )
            updateStatement.setInt(1, amount);
            updateStatement.setString(2, OrderStatus.WAITING.name);
            updateStatement.executeUpdate();
        } catch (e: SQLException) {
            throw handleError("Order status could not be updated.", e);
        }
    }

    fun updateMachineOccupation(machineId: Int, timestamp: Timestamp) {
        try {
            val updateStatement: PreparedStatement = dbConnection!!.prepareStatement(
                "UPDATE $MACHINES_TABLE_NAME SET $MACHINE_OCCUPIED_UNTIL = ? WHERE $MACHINE_ID = ?"
            )
            updateStatement.setTimestamp(1, timestamp);
            updateStatement.setInt(2, machineId);
            updateStatement.executeUpdate();
        } catch (e: SQLException) {
            throw handleError("Machine occupation could not be updated.", e);
        }
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
        private const val ORDER_PRIORITY = "PRIORITY"
        private const val ORDER_STATUS = "STATUS"

        private const val PRODUCTS_TABLE_NAME = "PRODUCTS"
        private const val PRODUCT_NAME = "PRODUCT_NAME"
        private const val PRODUCT_PRIORITY = "PRIORITY"
        private const val PRODUCT_RECIPE = "RECIPE"

        private const val ORDER_ITEMS_TABLE_NAME = "ORDER_ITEMS"

        private const val MACHINES_TABLE_NAME = "MACHINES"
        private const val MACHINE_ID = "MACHINE_ID"
        private const val MACHINE_NAME = "NAME"
        private const val MACHINE_OCCUPIED_UNTIL = "OCCUPIED_UNTIL"
        private const val MACHINE_STATUS = "STATUS"

        private const val MACHINE_PROCEDURES_TABLE_NAME = "MACHINE_PROCEDURES"
        private const val MACHINE_PROCEDURE = "MACHINE_PROCEDURE"

        private const val INSTRUCTIONS_TABLE_NAME = "INSTRUCTIONS"
        private const val INSTRUCTION_ID = "INSTRUCTION_ID"
        private const val INSTRUCTION_INGREDIENTS = "INGREDIENTS"
        private const val INSTRUCTION_DURATION = "DURATION"
    }
}