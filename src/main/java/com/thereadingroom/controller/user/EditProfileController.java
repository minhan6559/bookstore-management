package com.thereadingroom.controller.user;

import com.thereadingroom.model.entity.User;
import com.thereadingroom.service.user.IUserService;
import com.thereadingroom.utils.auth.SessionManager;
import com.thereadingroom.utils.ui.UIUtils;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

/**
 * Controller responsible for handling the edit profile functionality.
 * Provides the ability to update user profile information such as username,
 * first name, last name, and password.
 */
public class EditProfileController {

    @FXML
    private TextField usernameField; // Field to display and edit the username

    @FXML
    private TextField firstNameField; // Field to display and edit the first name

    @FXML
    private TextField lastNameField; // Field to display and edit the last name

    @FXML
    private PasswordField passwordField; // Field to display and edit the password

    @FXML
    private PasswordField confirmPasswordField; // Field to confirm the password

    private IUserService userService; // Service for user-related operations

    /**
     * Dependency injection for the user service.
     *
     * @param userService The service that handles user operations.
     */
    public void setUserService(IUserService userService) {
        this.userService = userService;
    }

    @FXML
    public void initialize() {
        // Populate the input fields with the session data on initialization
        populateFieldsWithSessionData();
    }

    /**
     * Populates the input fields with the current session data.
     * This method is called during the initialization of the controller.
     */
    private void populateFieldsWithSessionData() {
        usernameField.setText(getSessionUsername()); // Set the username from session
        firstNameField.setText(getSessionFirstName()); // Set the first name from session
        lastNameField.setText(getSessionLastName()); // Set the last name from session
    }

    /**
     * Handles the save changes button click.
     * Validates the input fields and proceeds to update the user profile if
     * validation passes.
     */
    @FXML
    public void handleSaveChanges() {
        if (validateFields()) {
            updateUserProfile(); // Proceed to update user profile if input is valid
        }
    }

    /**
     * Validates that all input fields are filled.
     *
     * @return true if all fields are filled, false otherwise.
     */
    private boolean validateFields() {
        if (isAnyFieldEmpty()) {
            UIUtils.showError("Validation Error", "All fields must be filled out.");
            return false;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            UIUtils.showError("Validation Error", "Passwords do not match.");
            return false;
        }
        return true;
    }

    /**
     * Checks if any of the input fields (first name, last name, or password) are
     * empty.
     *
     * @return true if any field is empty, false otherwise.
     */
    private boolean isAnyFieldEmpty() {
        return firstNameField.getText().isEmpty() ||
                lastNameField.getText().isEmpty() ||
                passwordField.getText().isEmpty() ||
                confirmPasswordField.getText().isEmpty();
    }

    /**
     * Updates the user profile in the system.
     * Fetches the current user from the database, validates the current session,
     * and updates the profile with new data.
     */
    private void updateUserProfile() {
        String currentUsername = getSessionUsername(); // Retrieve the username from the session
        String newFirstName = firstNameField.getText(); // Get new first name from input
        String newLastName = lastNameField.getText(); // Get new last name from input
        String newPassword = passwordField.getText(); // Get new password from input

        // Retrieve the current user from the database using the session username
        Optional<User> currentUserOpt = userService.getUserByUsername(currentUsername);
        if (currentUserOpt.isPresent()) {
            User currentUser = currentUserOpt.get();

            // Attempt to update the user profile with the new details
            if (userService.updateUserProfile(currentUsername, newFirstName, newLastName, newPassword,
                    currentUser.isAdmin())) {
                // Update the session data with the new user details
                updateSessionData(currentUser, newFirstName, newLastName);
                UIUtils.showAlert("Success", "Profile updated successfully.");
            } else {
                UIUtils.showError("Error", "Failed to update profile.");
            }
        } else {
            UIUtils.showError("Error", "User data not found.");
        }
    }

    /**
     * Updates the session data with the new profile details.
     * This ensures that the session reflects the latest changes in the user's
     * profile.
     *
     * @param user         The user object.
     * @param newFirstName The updated first name.
     * @param newLastName  The updated last name.
     */
    private void updateSessionData(User user, String newFirstName, String newLastName) {
        SessionManager.getInstance().setUserDetails(user.getId(), user.getUsername(), newFirstName, newLastName);
    }

    // Utility methods to retrieve session data

    private String getSessionUsername() {
        return SessionManager.getInstance().getUsername();
    }

    private String getSessionFirstName() {
        return SessionManager.getInstance().getFirstName();
    }

    private String getSessionLastName() {
        return SessionManager.getInstance().getLastName();
    }
}
