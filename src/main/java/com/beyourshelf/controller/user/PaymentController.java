package com.beyourshelf.controller.user;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.beyourshelf.model.entity.Book;
import com.beyourshelf.model.entity.Order;
import com.beyourshelf.model.entity.OrderItem;
import com.beyourshelf.model.entity.ShoppingCart;
import com.beyourshelf.service.ServiceManager;
import com.beyourshelf.service.cart.ICartService;
import com.beyourshelf.service.order.IOrderService;
import com.beyourshelf.service.payment.IPaymentService;
import com.beyourshelf.utils.auth.PaymentValidator;
import com.beyourshelf.utils.ui.UIUtils;

/**
 * Controller responsible for handling payment functionality.
 * Manages the process of validating payment details, processing payments,
 * saving orders, and clearing the shopping cart.
 */
public class PaymentController {

    @FXML
    private TextField cardNumberField; // TextField for entering card number

    @FXML
    private TextField cardHolderNameField; // TextField for entering the cardholder's name

    @FXML
    private TextField expiryDateField; // TextField for entering the card's expiry date

    @FXML
    private TextField cvvField; // TextField for entering the card's CVV

    @FXML
    private Label totalAmountLabel; // Label to display the total amount to be paid

    private double totalAmount; // Total amount of the order
    private int userId; // ID of the user making the purchase
    private ShoppingCart shoppingCart; // The user's shopping cart

    // Service dependencies for handling orders, cart operations, and payments
    private final IOrderService orderService = ServiceManager.getInstance().getOrderService();
    private final ICartService cartService = ServiceManager.getInstance().getCartService();
    private final IPaymentService paymentService = ServiceManager.getInstance().getPaymentService();

    private ShoppingCartController shoppingCartController; // Controller responsible for managing the shopping cart UI
    private boolean paymentCompleted = false; // Track whether payment finished successfully

    /**
     * Sets the payment details and displays the total amount.
     *
     * @param totalAmount            The total amount for the payment.
     * @param userId                 The ID of the user making the payment.
     * @param shoppingCart           The shopping cart associated with the user.
     * @param shoppingCartController The controller managing the shopping cart.
     */
    public void setPaymentDetails(double totalAmount, int userId, ShoppingCart shoppingCart,
            ShoppingCartController shoppingCartController) {
        this.totalAmount = totalAmount;
        this.userId = userId;
        this.shoppingCart = shoppingCart;
        this.shoppingCartController = shoppingCartController;
        updateTotalAmountLabel(); // Update the displayed total amount
        setupCloseHandler(); // Ensure reservations are reverted on cancel/close
    }

    /**
     * Updates the label displaying the total amount to be paid.
     */
    private void updateTotalAmountLabel() {
        totalAmountLabel.setText("Total Amount: $" + String.format("%.2f", totalAmount)); // Format and display total
                                                                                          // amount
    }

    /**
     * Handles the payment process when the user submits their payment details.
     * Validates the payment details, processes the payment, saves the order, and
     * clears the cart if successful.
     */
    @FXML
    public void handlePayment() {
        if (!validatePaymentDetails())
            return; // Validate payment details before proceeding

        try {
            String orderReference = processPaymentDetails(); // Process payment and retrieve order reference
            saveOrder(orderReference); // Save the order in the database
            UIUtils.showAlert("Payment Successful", "Your payment was successful! Order Reference: " + orderReference); // Notify
                                                                                                                        // user
                                                                                                                        // of
                                                                                                                        // success
            paymentCompleted = true;
            shoppingCartController.finalizeStockAfterPayment(); // Finalize stock changes after payment
            closePaymentScreen(); // Close the payment screen
        } catch (SQLException e) {
            handlePaymentError(); // Handle any errors that occur during payment
        }
    }

    /**
     * Validates the payment details entered by the user.
     *
     * @return true if all details are valid, false otherwise.
     */
    private boolean validatePaymentDetails() {
        String cardNumberError = PaymentValidator.validateCardNumber(cardNumberField.getText());
        String expiryDateError = PaymentValidator.validateExpiryDate(expiryDateField.getText());
        String cvvError = PaymentValidator.validateCVV(cvvField.getText());

        if (cardNumberError != null) {
            UIUtils.showError("Payment Failed", cardNumberError);
            return false;
        }
        if (expiryDateError != null) {
            UIUtils.showError("Payment Failed", expiryDateError);
            return false;
        }
        if (cvvError != null) {
            UIUtils.showError("Payment Failed", cvvError);
            return false;
        }
        return true; // All details are valid
    }

    /**
     * Processes the payment details by calling the payment service.
     *
     * @return A unique order reference for the payment.
     * @throws SQLException if payment processing fails.
     */
    private String processPaymentDetails() throws SQLException {
        String cardNumber = cardNumberField.getText();
        String cardHolderName = cardHolderNameField.getText();
        String expiryDate = expiryDateField.getText();
        String cvv = cvvField.getText();

        // Process payment and return order reference; throw SQLException if the payment
        // fails
        return paymentService.processPayment(cardNumber, cardHolderName, expiryDate, cvv)
                .orElseThrow(() -> new SQLException("Payment processing failed."));
    }

    /**
     * Saves the order details in the database.
     *
     * @param orderReference The unique reference for the order.
     * @throws SQLException if order saving fails.
     */
    private void saveOrder(String orderReference) throws SQLException {
        List<OrderItem> orderItems = createOrderItems(); // Create the list of order items
        Order order = new Order(orderReference, userId, totalAmount, orderItems); // Create a new Order object

        // Place the order using the order service; throw SQLException if the order
        // saving fails
        if (!orderService.placeOrder(order)) {
            throw new SQLException("Error saving order.");
        }
    }

    /**
     * Creates a list of order items based on the books in the shopping cart.
     *
     * @return A list of OrderItem objects.
     */
    private List<OrderItem> createOrderItems() {
        List<OrderItem> orderItems = new ArrayList<>();
        // Only process selected items from the shopping cart
        for (Map.Entry<Book, Integer> entry : shoppingCart.getBooks().entrySet()) {
            Book book = entry.getKey();
            int quantity = entry.getValue();
            if (shoppingCartController.isBookSelected(book)) {
                orderItems.add(new OrderItem(book.getBookId(), book.getTitle(), quantity, book.getPrice()));
            }
        }
        return orderItems;
    }

    /**
     * Handles any errors that occur during the payment process.
     */
    private void handlePaymentError() {
        UIUtils.showError("Payment Failed", "An error occurred during payment. Please try again."); // Display error
                                                                                                    // message
        shoppingCartController.revertReservedStock(); // Revert any stock that was reserved for the payment
    }

    /**
     * Closes the payment screen after the process is complete.
     */
    private void closePaymentScreen() {
        Stage stage = (Stage) totalAmountLabel.getScene().getWindow(); // Get the current window
        stage.close(); // Close the window
    }

    /**
     * Ensure that if the user closes the payment window (cancel), we revert any
     * previously reserved stock.
     */
    private void setupCloseHandler() {
        // Wait until the control is attached to a scene/window
        totalAmountLabel.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin instanceof Stage stage) {
                        stage.setOnCloseRequest(event -> {
                            if (!paymentCompleted) {
                                shoppingCartController.revertReservedStock();
                            }
                        });
                    }
                });
            }
        });
    }
}
