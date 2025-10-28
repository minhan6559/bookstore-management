package com.thereadingroom.controller.admin;

import com.thereadingroom.model.entity.User;
import com.thereadingroom.service.user.IUserService;
import com.thereadingroom.service.ServiceManager;
import com.thereadingroom.utils.ui.UIUtils;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller class for editing a user profile in the admin panel.
 * It allows the admin to modify a user's details and save the changes via the
 * IUserService.
 */
public class AdminEditUserController {

    @FXML
    private TextField usernameField; // Input field for the user's username

    @FXML
    private TextField firstNameField; // Input field for the user's first name

    @FXML
    private TextField lastNameField; // Input field for the user's last name

    @FXML
    private PasswordField passwordField; // Input field for the user's password

    @FXML
    private PasswordField confirmPasswordField; // Confirm password field

    // Service for handling user-related operations
    private final IUserService userService = ServiceManager.getInstance().getUserService();

    private User user; // Holds the user object that is being edited

    /**
     * Set the user whose profile is being edited.
     * This method is called to pass the selected user's details to the controller
     * and populate the form fields.
     *
     * @param user The user whose profile is being edited.
     */
    public void setUser(User user) {
        this.user = user;
        populateUserDetails(); // Populate form fields with user details
    }

    /**
     * Populates the form fields with the user's current profile information.
     * Called when the user details are passed to the controller.
     */
    private void populateUserDetails() {
        usernameField.setText(user.getUsername());
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        passwordField.setText(user.getPassword()); // Password stored here for editing (hashed in actual implementation)
    }

    /**
     * Handles saving the updated user profile. Validates input fields, updates the
     * user entity,
     * and calls the service to persist the changes.
     */
    @FXML
    public void handleSaveUser() {
        if (validateInputFields()) {
            // Attempt to update the user profile with the new values
            boolean success = userService.updateUserProfilebyID(
                    user.getId(), // User ID remains unchanged
                    usernameField.getText().trim(),
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    passwordField.getText().trim(), // Updated password
                    user.isAdmin() // Retain admin status
            );

            // Provide feedback based on the result of the update operation
            if (success) {
                UIUtils.showAlert("Success", "User profile updated successfully."); // Show success message
                UIUtils.closeCurrentWindow(usernameField); // Close the window
            } else {
                UIUtils.showError("Update Failed", "Failed to update user profile."); // Show error message
            }
        }
    }

    /**
     * Validates the input fields to ensure no field is left empty.
     * Displays an error message if any required field is missing.
     *
     * @return boolean indicating if the fields are valid (true) or not (false)
     */
    private boolean validateInputFields() {
        if (usernameField.getText().trim().isEmpty() ||
                firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                passwordField.getText().trim().isEmpty() ||
                confirmPasswordField.getText().trim().isEmpty()) {

            // Display an error if any field is left empty
            UIUtils.showError("Validation Error", "All fields must be filled.");
            return false;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            UIUtils.showError("Validation Error", "Passwords do not match.");
            return false;
        }
        return true;
    }

    /**
     * Handles the cancel action. Closes the current window without saving any
     * changes.
     */
    @FXML
    public void handleCancel() {
        UIUtils.closeCurrentWindow(usernameField); // Close the window without saving changes
    }
}
