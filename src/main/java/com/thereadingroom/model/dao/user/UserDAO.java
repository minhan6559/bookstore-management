package com.thereadingroom.model.dao.user;

import com.thereadingroom.model.dao.BaseDAO;
import com.thereadingroom.model.entity.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User Data Access Object (DAO) implementation.
 * Provides methods to interact with the users table in the database.
 */
public class UserDAO extends BaseDAO implements IUserDAO {

    /**
     * Fetches all users from the database.
     *
     * @return A list of all User objects.
     */
    @Override
    public List<User> getAllUsers() {
        String query = "SELECT id, username, first_name, last_name, is_admin FROM users";
        return executeQuery(query, rs -> {
            try {
                return mapToUserList(rs); // Maps result set to a list of users
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Registers a new user in the database.
     *
     * @param username  The username of the user.
     * @param firstName The first name of the user.
     * @param lastName  The last name of the user.
     * @param password  The password for the user.
     * @param isAdmin   Whether the user is an admin.
     * @return true if the registration is successful, false otherwise.
     */
    @Override
    public boolean registerUser(String username, String firstName, String lastName, String password, boolean isAdmin) {
        String sql = "INSERT INTO users (username, first_name, last_name, password, is_admin) VALUES (?, ?, ?, ?, ?)";
        return executeUpdate(sql, username, firstName, lastName, password, isAdmin);
    }

    /**
     * Validates login credentials for a user.
     *
     * @param username The username of the user.
     * @param password The password of the user.
     * @return true if the credentials are valid, false otherwise.
     * @deprecated This method performs plaintext password comparison, which is
     *             insecure.
     *             Passwords are stored as bcrypt hashes in the database, so this
     *             method will
     *             always return false. Use {@link #getUserByUsername(String)}
     *             instead and
     *             verify the password using bcrypt in the service layer.
     * @see com.thereadingroom.service.user.UserService#validateUserLogin(String,
     *      String)
     */
    @Deprecated
    @Override
    public boolean validateLogin(String username, String password) {
        // WARNING: This method compares plaintext passwords, but passwords in the
        // database
        // are stored as bcrypt hashes. This method will always return false.
        // Use getUserByUsername() and verify password with bcrypt in the service layer
        // instead.
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        return executeQuery(sql, rs -> {
            try {
                return rs.next(); // Check if the user exists in the result set
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, username, password);
    }

    /**
     * Fetches a user by their username.
     *
     * @param username The username of the user.
     * @return A User object if the user is found, null otherwise.
     */
    @Override
    public User getUserByUsername(String username) {
        String sql = "SELECT id, username, first_name, last_name, password, is_admin FROM users WHERE username = ?";
        return executeQuery(sql, rs -> {
            try {
                return mapToUser(rs); // Maps result set to a User object
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, username);
    }

    /**
     * Updates a user's profile based on their username.
     *
     * @param username  The username of the user.
     * @param firstName The new first name.
     * @param lastName  The new last name.
     * @param password  The new password.
     * @param isAdmin   Whether the user is an admin.
     * @return true if the update is successful, false otherwise.
     */
    @Override
    public boolean updateUserProfile(String username, String firstName, String lastName, String password,
            boolean isAdmin) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, password = ?, is_admin = ? WHERE username = ?";
        return executeUpdate(sql, firstName, lastName, password, isAdmin, username);
    }

    /**
     * Checks if the user is an admin based on their username.
     *
     * @param username The username of the user.
     * @return true if the user is an admin, false otherwise.
     */
    @Override
    public boolean isAdminUser(String username) {
        String sql = "SELECT is_admin FROM users WHERE username = ?";
        return executeQuery(sql, rs -> {
            try {
                return rs.next() && rs.getBoolean("is_admin"); // Return whether the user is admin
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, username);
    }

    /**
     * Retrieves the user ID based on the username.
     *
     * @param username The username of the user.
     * @return The user ID, or -1 if the user is not found.
     */
    @Override
    public int getUserIdByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        return executeQuery(sql, rs -> {
            try {
                return rs.next() ? rs.getInt("id") : -1; // Return the user ID, or -1 if not found
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, username);
    }

    /**
     * Deletes a user by their user ID.
     *
     * @param userId The ID of the user to delete.
     * @return true if the user is deleted successfully, false otherwise.
     */
    @Override
    public boolean deleteUserById(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        return executeUpdate(sql, userId);
    }

    /**
     * Updates a user's profile based on their user ID.
     *
     * @param userId    The user ID of the user.
     * @param username  The new username.
     * @param firstName The new first name.
     * @param lastName  The new last name.
     * @param password  The new password.
     * @param isAdmin   Whether the user is an admin.
     * @return true if the update is successful, false otherwise.
     */
    @Override
    public boolean updateUserProfileById(int userId, String username, String firstName, String lastName,
            String password, boolean isAdmin) {
        String sql = "UPDATE users SET username = ?, first_name = ?, last_name = ?, password = ?, is_admin = ? WHERE id = ?";
        return executeUpdate(sql, username, firstName, lastName, password, isAdmin, userId);
    }

    /**
     * Maps a result set to a User object.
     *
     * @param rs The ResultSet object.
     * @return A User object if a result exists, null otherwise.
     * @throws SQLException If there is an issue with reading the result set.
     */
    private User mapToUser(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("password"),
                    rs.getBoolean("is_admin"));
        }
        return null; // Return null if no user is found
    }

    /**
     * Maps a result set to a list of User objects.
     *
     * @param rs The ResultSet object.
     * @return A list of User objects.
     * @throws SQLException If there is an issue with reading the result set.
     */
    private List<User> mapToUserList(ResultSet rs) throws SQLException {
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    "", // Omit the password for security
                    rs.getBoolean("is_admin")));
        }
        return users;
    }
}
