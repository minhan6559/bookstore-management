package com.thereadingroom.service.user;

import com.thereadingroom.model.dao.user.UserDAO;
import com.thereadingroom.model.entity.User;
import com.thereadingroom.utils.auth.PasswordUtils;

import java.util.List;
import java.util.Optional;

/**
 * UserService handles business logic related to user management.
 * It acts as an intermediary between controllers and the data access layer
 * (DAO).
 */
public class UserService implements IUserService {

    // Singleton instance of UserService
    private static UserService instance;

    // DAO to interact with the user database
    private final UserDAO userDAO;

    // Private constructor to prevent direct instantiation (Singleton pattern)
    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Get the singleton instance of UserService.
     *
     * @return The singleton instance of UserService.
     */
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    /**
     * Get the user ID for a given username.
     *
     * @param username The username to look up.
     * @return The user ID associated with the username.
     */
    @Override
    public int getUserIdByUsername(String username) {
        return userDAO.getUserIdByUsername(username);
    }

    /**
     * Validate the login credentials of a user.
     *
     * @param username The username provided.
     * @param password The password provided.
     * @return true if the credentials are valid; false otherwise.
     */
    @Override
    public boolean validateUserLogin(String username, String password) {
        User user = userDAO.getUserByUsername(username);
        if (user == null)
            return false;

        String storedPassword = user.getPassword();
        if (storedPassword == null)
            return false;

        // If already hashed, verify with bcrypt
        if (PasswordUtils.isBcryptHash(storedPassword)) {
            return PasswordUtils.verifyPassword(password, storedPassword);
        }

        // Fallback for legacy plaintext: compare, then migrate to hashed
        boolean matches = storedPassword.equals(password);
        if (matches) {
            String hashed = PasswordUtils.hashPassword(password);
            userDAO.updateUserProfileById(
                    user.getId(),
                    user.getUsername(),
                    user.getFirstName(),
                    user.getLastName(),
                    hashed,
                    user.isAdmin());
        }
        return matches;
    }

    /**
     * Check if a user is an admin.
     *
     * @param username The username of the user.
     * @return true if the user is an admin; false otherwise.
     */
    @Override
    public boolean isAdminUser(String username) {
        return userDAO.isAdminUser(username);
    }

    /**
     * Retrieve a user by their username.
     *
     * @param username The username to look up.
     * @return An Optional containing the User object if found, or empty if not
     *         found.
     */
    @Override
    public Optional<User> getUserByUsername(String username) {
        return Optional.ofNullable(userDAO.getUserByUsername(username));
    }

    /**
     * Update the profile of a user based on their username.
     *
     * @param username  The username of the user to update.
     * @param firstName The new first name.
     * @param lastName  The new last name.
     * @param password  The new password.
     * @param isAdmin   Whether the user is an admin.
     * @return true if the update was successful; false otherwise.
     */
    @Override
    public boolean updateUserProfile(String username, String firstName, String lastName, String password,
            boolean isAdmin) {
        String toStore = password;
        if (password == null || password.trim().isEmpty()) {
            // Preserve existing password
            User existing = userDAO.getUserByUsername(username);
            toStore = existing != null ? existing.getPassword() : null;
        } else if (!PasswordUtils.isBcryptHash(password)) {
            toStore = PasswordUtils.hashPassword(password);
        }
        return userDAO.updateUserProfile(username, firstName, lastName, toStore, isAdmin);
    }

    /**
     * Update the profile of a user based on their user ID.
     *
     * @param userId    The ID of the user to update.
     * @param username  The new username.
     * @param firstName The new first name.
     * @param lastName  The new last name.
     * @param password  The new password.
     * @param isAdmin   Whether the user is an admin.
     * @return true if the update was successful; false otherwise.
     */
    @Override
    public boolean updateUserProfilebyID(int userId, String username, String firstName, String lastName,
            String password, boolean isAdmin) {
        String toStore = password;
        if (password == null || password.trim().isEmpty()) {
            // Preserve existing password
            Optional<User> existingOpt = Optional.ofNullable(userDAO.getUserByUsername(username));
            toStore = existingOpt.map(User::getPassword).orElse(null);
        } else if (!PasswordUtils.isBcryptHash(password)) {
            toStore = PasswordUtils.hashPassword(password);
        }
        return userDAO.updateUserProfileById(userId, username, firstName, lastName, toStore, isAdmin);
    }

    /**
     * Register a new user.
     *
     * @param username  The new user's username.
     * @param firstName The new user's first name.
     * @param lastName  The new user's last name.
     * @param password  The new user's password.
     * @param isAdmin   Whether the new user is an admin.
     * @return true if the registration was successful; false otherwise.
     */
    @Override
    public boolean registerUser(String username, String firstName, String lastName, String password, boolean isAdmin) {
        String hashed = PasswordUtils.hashPassword(password);
        return userDAO.registerUser(username, firstName, lastName, hashed, isAdmin);
    }

    /**
     * Retrieve all users.
     *
     * @return A list of all users.
     */
    @Override
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    /**
     * Delete a user by their user ID.
     *
     * @param userId The ID of the user to delete.
     * @return true if the deletion was successful; false otherwise.
     */
    public boolean deleteUser(int userId) {
        return userDAO.deleteUserById(userId);
    }
}
