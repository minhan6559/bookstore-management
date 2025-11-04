package com.beyourshelf.model.dao.cart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.beyourshelf.model.dao.BaseDAO;
import com.beyourshelf.model.dao.database.Database;
import com.beyourshelf.model.entity.Book;
import com.beyourshelf.model.entity.CartItem;

/**
 * DAO implementation for managing cart-related operations.
 * Handles operations like creating a cart, adding/removing items, checking
 * stock, etc.
 */
public class CartDAO extends BaseDAO implements ICartDAO {

    /**
     * Retrieves the active cart for a user, or creates a new one if no active cart
     * exists.
     * 
     * @param userId the user ID.
     * @return the cart ID.
     */
    @Override
    public int getOrCreateCart(int userId) {
        int cartId = getActiveCart(userId);
        if (cartId == -1) {
            cartId = createNewCart(userId);
        }
        return cartId;
    }

    /**
     * Fetches the active cart for a user.
     * 
     * @param userId the user ID.
     * @return the cart ID if found, -1 if not found.
     */
    private int getActiveCart(int userId) {
        String sql = "SELECT cart_id FROM cart WHERE user_id = ? AND status = 'active'";
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("cart_id");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching active cart: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Creates a new active cart for the given user.
     * 
     * @param userId the user ID.
     * @return the newly created cart ID.
     */
    private int createNewCart(int userId) {
        String sql = "INSERT INTO cart (user_id, status) VALUES (?, 'active')";
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error creating new cart: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves all items in a cart.
     * 
     * @param cartId the cart ID.
     * @return a list of CartItem objects.
     */
    @Override
    public List<CartItem> getCartItems(int cartId) {
        List<CartItem> cartItems = new ArrayList<>();
        String sql = "SELECT ci.book_id, ci.quantity " +
                "FROM cart_items ci " +
                "WHERE ci.cart_id = ?";
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cartId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                CartItem item = new CartItem(
                        rs.getInt("book_id"),
                        rs.getInt("quantity"));
                cartItems.add(item);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching cart items: " + e.getMessage());
        }
        return cartItems;
    }

    /**
     * Removes a book from the cart.
     * 
     * @param cartId the cart ID.
     * @param bookId the book ID to remove.
     */
    @Override
    public void removeBookFromCart(int cartId, int bookId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND book_id = ?";
        executeUpdate(sql, cartId, bookId);
    }

    /**
     * Updates the quantity of a book in the cart.
     * 
     * @param cartId   the cart ID.
     * @param bookId   the book ID.
     * @param quantity the new quantity.
     */
    @Override
    public void updateBookQuantity(int cartId, int bookId, int quantity) {
        String sql = "UPDATE cart_items SET quantity = ? WHERE cart_id = ? AND book_id = ?";
        executeUpdate(sql, quantity, cartId, bookId);
    }

    /**
     * Adds or updates a book in the cart.
     * 
     * @param cartId   the cart ID.
     * @param bookId   the book ID.
     * @param quantity the quantity to add or update.
     */
    @Override
    public void addOrUpdateBookInCart(int cartId, int bookId, int quantity) {
        String sql = "INSERT INTO cart_items (cart_id, book_id, quantity) VALUES (?, ?, ?) "
                + "ON CONFLICT(cart_id, book_id) DO UPDATE SET quantity = quantity + excluded.quantity";
        executeUpdate(sql, cartId, bookId, quantity);
    }

    /**
     * Removes selected books from the cart.
     *
     * @param cartId        The ID of the cart.
     * @param booksToRemove The list of books to remove from the cart.
     */
    public void removeBooksFromCart(int cartId, List<Book> booksToRemove) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND book_id = ?";
        try (Connection conn = Database.getInstance().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Book book : booksToRemove) {
                pstmt.setInt(1, cartId);
                pstmt.setInt(2, book.getBookId());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.out.println("Error removing books from cart: " + e.getMessage());
        }
    }

}
