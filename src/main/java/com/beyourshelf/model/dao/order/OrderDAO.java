package com.beyourshelf.model.dao.order;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.beyourshelf.model.dao.BaseDAO;
import com.beyourshelf.model.dao.database.Database;
import com.beyourshelf.model.entity.Order;
import com.beyourshelf.model.entity.OrderItem;

/**
 * Implementation of the IOrderDAO interface for interacting with the orders
 * table in the database.
 * Provides methods to save, retrieve, and delete orders.
 */
public class OrderDAO extends BaseDAO implements IOrderDAO {

    /**
     * Saves an order in the database along with its associated order items.
     * Uses a transaction to ensure atomicity of the operation.
     *
     * @param order The Order object to save.
     * @return true if the order is saved successfully, false otherwise.
     */
    @Override
    public boolean saveOrder(Order order) {
        String insertOrderSQL = "INSERT INTO orders (order_number, user_id, total_price, order_date) VALUES (?, ?, ?, ?)";
        String insertOrderItemSQL = "INSERT INTO order_items (order_id, book_id, title, quantity, price) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            // Insert the order and retrieve the generated order ID
            int orderId = insertOrder(conn, insertOrderSQL, order);

            if (orderId != -1) {
                order.setOrderId(orderId);
                // Insert associated order items
                insertOrderItems(conn, insertOrderItemSQL, order.getOrderItems(), orderId);
                conn.commit(); // Commit transaction
                return true;
            }

        } catch (SQLException e) {
            System.out.println("Error saving order: " + e.getMessage());
        }

        return false;
    }

    /**
     * Inserts the order into the database and returns the generated order ID.
     *
     * @param conn  The database connection.
     * @param sql   The SQL query to insert the order.
     * @param order The Order object.
     * @return The generated order ID, or -1 if insertion failed.
     */
    private int insertOrder(Connection conn, String sql, Order order) throws SQLException {
        try (PreparedStatement orderStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            orderStmt.setString(1, order.getOrderNumber());
            orderStmt.setInt(2, order.getUserId());
            orderStmt.setDouble(3, order.getTotalPrice());
            orderStmt.setTimestamp(4, Timestamp.valueOf(order.getOrderDate()));
            orderStmt.executeUpdate();

            ResultSet rs = orderStmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // Return the generated order ID
            }
        }
        return -1;
    }

    /**
     * Inserts the list of order items associated with an order.
     *
     * @param conn       The database connection.
     * @param sql        The SQL query to insert the order items.
     * @param orderItems The list of order items.
     * @param orderId    The ID of the order the items are associated with.
     */
    private void insertOrderItems(Connection conn, String sql, List<OrderItem> orderItems, int orderId)
            throws SQLException {
        try (PreparedStatement itemStmt = conn.prepareStatement(sql)) {
            for (OrderItem item : orderItems) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, item.getBookId());
                itemStmt.setString(3, item.getTitle());
                itemStmt.setInt(4, item.getQuantity());
                itemStmt.setDouble(5, item.getPrice());
                itemStmt.addBatch(); // Add each item to the batch
            }
            itemStmt.executeBatch(); // Execute the batch
        }
    }

    /**
     * Retrieves all orders placed by a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of Order objects belonging to the user.
     */
    @Override
    public List<Order> getAllOrdersByUser(int userId) {
        String fetchOrdersSQL = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC";
        return fetchOrders(fetchOrdersSQL, userId);
    }

    /**
     * Retrieves an order by its unique ID.
     *
     * @param orderId The ID of the order to retrieve.
     * @return An Optional containing the Order if found, or empty if not found.
     */
    @Override
    public Optional<Order> getOrderById(int orderId) {
        String fetchOrderSQL = "SELECT * FROM orders WHERE order_id = ?";
        return fetchSingleOrder(fetchOrderSQL, orderId);
    }

    /**
     * Retrieves a list of selected orders by a user based on the provided order
     * IDs.
     *
     * @param userId   The ID of the user.
     * @param orderIds The list of order IDs to retrieve.
     * @return A list of Order objects that match the provided IDs.
     */
    @Override
    public List<Order> getSelectedOrdersByUser(int userId, List<Integer> orderIds) {
        String orderIdsSQL = buildPlaceholders(orderIds);
        String fetchOrdersSQL = "SELECT * FROM orders WHERE user_id = ? AND order_id IN (" + orderIdsSQL + ")";
        return fetchOrders(fetchOrdersSQL, userId, orderIds);
    }

    public List<Order> getAdminSelectedOrdersByIds(List<Integer> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Construct the SQL query with placeholders for order IDs
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM orders WHERE order_id IN (");
        queryBuilder.append(orderIds.stream().map(id -> "?").collect(Collectors.joining(",")));
        queryBuilder.append(")");

        String query = queryBuilder.toString();

        try (Connection connection = Database.getInstance().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Set the order IDs in the prepared statement
            for (int i = 0; i < orderIds.size(); i++) {
                preparedStatement.setInt(i + 1, orderIds.get(i));
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            return mapOrdersForAdmin(resultSet); // Assuming mapOrdersForAdmin maps ResultSet to Order objects with
                                                 // admin fields
        } catch (SQLException e) {
            System.err.println("Error fetching selected orders for admin: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // This method will map ResultSet to Order objects specifically for admin
    private List<Order> mapOrdersForAdmin(ResultSet resultSet) throws SQLException {
        List<Order> orders = new ArrayList<>();
        while (resultSet.next()) {
            // Create the Order object using the constructor or setters
            Order order = new Order(); // Use the no-argument constructor for flexibility

            // Set all the fields required for admin
            order.setOrderId(resultSet.getInt("order_id"));
            order.setUserId(resultSet.getInt("user_id"));
            order.setOrderNumber(resultSet.getString("order_number"));
            order.setTotalPrice(resultSet.getDouble("total_price"));

            // Convert LocalDate to LocalDateTime (assuming the order_date is stored as a
            // LocalDate)
            LocalDate orderDate = resultSet.getDate("order_date").toLocalDate();
            order.setOrderDate(orderDate.atStartOfDay()); // Convert LocalDate to LocalDateTime

            // Fetch the order items
            order.setOrderItems(fetchOrderItems(order.getOrderId(), resultSet.getStatement().getConnection()));

            orders.add(order);
        }
        return orders;
    }

    /**
     * Retrieves all orders from the database.
     *
     * @return A list of all Order objects.
     */
    @Override
    public List<Order> getAllOrders() {
        String fetchOrdersSQL = "SELECT * FROM orders ORDER BY order_date DESC";
        return fetchOrders(fetchOrdersSQL, -1);
    }

    /**
     * Retrieves a list of selected orders by their IDs.
     *
     * @param orderIds The list of order IDs to retrieve.
     * @return A list of Order objects that match the provided IDs.
     */
    @Override
    public List<Order> getSelectedOrdersByIds(List<Integer> orderIds) {
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        String orderIdsSQL = buildPlaceholders(orderIds);
        String fetchOrdersSQL = "SELECT * FROM orders WHERE order_id IN (" + orderIdsSQL + ")";
        return fetchOrders(fetchOrdersSQL, -1, orderIds);
    }

    /**
     * Retrieves a single order based on the SQL query and order ID.
     *
     * @param fetchOrderSQL The SQL query to fetch the order.
     * @param orderId       The ID of the order to retrieve.
     * @return An Optional containing the Order if found.
     */
    private Optional<Order> fetchSingleOrder(String fetchOrderSQL, int orderId) {
        return fetchOrders(fetchOrderSQL, -1, List.of(orderId)).stream().findFirst();
    }

    /**
     * Fetches a list of orders based on the provided SQL query and user ID.
     *
     * @param sql    The SQL query to execute.
     * @param userId The ID of the user, or -1 for all users.
     * @return A list of Order objects.
     */
    private List<Order> fetchOrders(String sql, int userId) {
        return fetchOrders(sql, userId, null);
    }

    /**
     * Fetches a list of orders based on the provided SQL query, user ID, and order
     * IDs.
     *
     * @param sql      The SQL query to execute.
     * @param userId   The ID of the user, or -1 for all users.
     * @param orderIds A list of order IDs to filter by.
     * @return A list of Order objects.
     */
    private List<Order> fetchOrders(String sql, int userId, List<Integer> orderIds) {
        List<Order> orders = new ArrayList<>();

        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement orderStmt = conn.prepareStatement(sql)) {

            if (userId != -1) {
                orderStmt.setInt(1, userId); // Set userId parameter
            } else if (orderIds != null) {
                setPreparedStatementParams(orderStmt, orderIds.toArray()); // Set order ID parameters
            }

            ResultSet orderRs = orderStmt.executeQuery();
            while (orderRs.next()) {
                Order order = extractOrder(orderRs);
                order.setOrderItems(fetchOrderItems(order.getOrderId(), conn));
                orders.add(order);
            }

        } catch (SQLException e) {
            System.out.println("Error fetching orders: " + e.getMessage());
        }

        return orders;
    }

    public List<Order> fetchOrdersForAdmin(List<Integer> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Construct the SQL query for admin orders
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM orders WHERE order_id IN (");
        queryBuilder.append(orderIds.stream().map(id -> "?").collect(Collectors.joining(",")));
        queryBuilder.append(")");

        String query = queryBuilder.toString();

        try (Connection connection = Database.getInstance().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Set the order IDs in the prepared statement
            for (int i = 0; i < orderIds.size(); i++) {
                preparedStatement.setInt(i + 1, orderIds.get(i));
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            return mapOrdersForAdmin(resultSet); // Map the result set to Order objects specifically for admin

        } catch (SQLException e) {
            System.err.println("Error fetching orders for admin: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches the order items associated with a specific order ID.
     *
     * @param orderId The ID of the order.
     * @param conn    The database connection.
     * @return A list of OrderItem objects.
     */
    private List<OrderItem> fetchOrderItems(int orderId, Connection conn) throws SQLException {
        String fetchOrderItemsSQL = "SELECT * FROM order_items WHERE order_id = ?";
        List<OrderItem> orderItems = new ArrayList<>();

        try (PreparedStatement itemStmt = conn.prepareStatement(fetchOrderItemsSQL)) {
            itemStmt.setInt(1, orderId);
            ResultSet itemRs = itemStmt.executeQuery();
            while (itemRs.next()) {
                orderItems.add(new OrderItem(
                        itemRs.getInt("book_id"),
                        itemRs.getString("title"),
                        itemRs.getInt("quantity"),
                        itemRs.getDouble("price")));
            }
        }
        return orderItems;
    }

    /**
     * Constructs an Order object from a ResultSet.
     *
     * @param orderRs The ResultSet containing order data.
     * @return An Order object.
     * @throws SQLException If an error occurs while extracting the order data.
     */
    private Order extractOrder(ResultSet orderRs) throws SQLException {
        Order order = new Order(
                orderRs.getString("order_number"),
                orderRs.getInt("user_id"),
                orderRs.getDouble("total_price"),
                new ArrayList<>() // Initialize with an empty list of order items
        );
        order.setOrderId(orderRs.getInt("order_id")); // Set the orderId
        return order;
    }

    /**
     * Builds a comma-separated list of placeholders for SQL IN clauses.
     *
     * @param ids A list of IDs.
     * @return A string of placeholders for the SQL IN clause.
     */
    private String buildPlaceholders(List<Integer> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));
    }

    /**
     * Deletes an order and its associated items by order ID.
     *
     * @param orderId The ID of the order to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteOrderById(int orderId) {
        String deleteOrderItemsSQL = "DELETE FROM order_items WHERE order_id = ?";
        String deleteOrderSQL = "DELETE FROM orders WHERE order_id = ?";

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false); // Begin transaction

            // Delete order items first
            try (PreparedStatement itemStmt = conn.prepareStatement(deleteOrderItemsSQL)) {
                itemStmt.setInt(1, orderId);
                int affectedItems = itemStmt.executeUpdate(); // Get affected rows count for order items
                if (affectedItems == 0) {
                    System.out.println("No order items found for order ID: " + orderId);
                    conn.rollback();
                    return false;
                }
            }

            // Then delete the order
            try (PreparedStatement orderStmt = conn.prepareStatement(deleteOrderSQL)) {
                orderStmt.setInt(1, orderId);
                int affectedRows = orderStmt.executeUpdate();

                if (affectedRows > 0) {
                    conn.commit(); // Commit the transaction if successful
                    return true;
                } else {
                    System.out.println("Order ID not found for deletion: " + orderId);
                }
            }

            conn.rollback(); // Rollback in case of any error
        } catch (SQLException e) {
            System.out.println("Error deleting order: " + e.getMessage());
            e.printStackTrace(); // Added detailed stack trace for better debugging
        }
        return false;
    }

}
