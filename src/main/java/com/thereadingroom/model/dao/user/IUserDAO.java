package com.thereadingroom.model.dao.user;

import com.thereadingroom.model.entity.User;

import java.util.List;

/**
 * Interface for User Data Access Object (DAO).
 * Defines methods for interacting with the users table in the database.
 */
public interface IUserDAO {

    /**
     * Retrieves all users from the database.
     *
     * @return A list of User objects.
     */
    List<User> getAllUsers();

    /**
     * Registers a new user in the database.
     *
     * @param username  The username of the user.
     * @param firstName The first name of the user.
     * @param lastName  The last name of the user.
     * @param password  The password for the user.
     * @param isAdmin   Whether the user is an admin (true for admin, false for
     *                  regular user).
     * @return true if the user is registered successfully, false otherwise.
     */
    boolean registerUser(String username, String firstName, String lastName, String password, boolean isAdmin);

    /**
     * Validates the login credentials for a user.
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
    boolean validateLogin(String username, String password);

    /**
     * Retrieves a user by their username.
     *
     * @param username The username of the user.
     * @return A User object if the user exists, null otherwise.
     */
    User getUserByUsername(String username);

    /**
     * Updates the profile of a user based on their username.
     *
     * @param username  The username of the user.
     * @param firstName The new first name of the user.
     * @param lastName  The new last name of the user.
     * @param password  The new password for the user.
     * @param isAdmin   Whether the user is an admin.
     * @return true if the profile is updated successfully, false otherwise.
     */
    boolean updateUserProfile(String username, String firstName, String lastName, String password, boolean isAdmin);

    /**
     * Checks if a user is an admin.
     *
     * @param username The username of the user.
     * @return true if the user is an admin, false otherwise.
     */
    boolean isAdminUser(String username);

    /**
     * Retrieves the user ID based on their username.
     *
     * @param username The username of the user.
     * @return The user ID if found, -1 otherwise.
     */
    int getUserIdByUsername(String username);

    /**
     * Deletes a user from the database based on their user ID.
     *
     * @param userId The ID of the user to delete.
     * @return true if the user is deleted successfully, false otherwise.
     */
    boolean deleteUserById(int userId);

    /**
     * Updates the profile of a user based on their user ID.
     *
     * @param userId    The ID of the user.
     * @param username  The new username of the user.
     * @param firstName The new first name of the user.
     * @param lastName  The new last name of the user.
     * @param password  The new password for the user.
     * @param isAdmin   Whether the user is an admin.
     * @return true if the profile is updated successfully, false otherwise.
     */
    boolean updateUserProfileById(int userId, String username, String firstName, String lastName, String password,
            boolean isAdmin);
}
