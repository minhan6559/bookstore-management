package com.beyourshelf.controller.user;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.beyourshelf.model.entity.Book;
import com.beyourshelf.model.entity.CartTableItem;
import com.beyourshelf.model.entity.ShoppingCart;
import com.beyourshelf.service.ServiceManager;
import com.beyourshelf.service.cart.CartService;
import com.beyourshelf.service.inventory.IInventoryService;
import com.beyourshelf.utils.auth.SessionManager;
import com.beyourshelf.utils.ui.UIUtils;

/**
 * Controller for managing the shopping cart UI and related operations.
 * Handles displaying cart items, quantity adjustment, checkout, and stock
 * management.
 */
public class ShoppingCartController {

    @FXML
    private TableView<CartTableItem> cartTableView; // TableView to display cart items

    @FXML
    private TableColumn<CartTableItem, Boolean> selectColumn; // Column for item selection checkbox

    @FXML
    private TableColumn<CartTableItem, String> itemNameColumn; // Column for item names

    @FXML
    private TableColumn<CartTableItem, Integer> quantityColumn; // Column for item quantities

    @FXML
    private TableColumn<CartTableItem, Double> totalAmountColumn; // Column for total item prices

    @FXML
    private TableColumn<CartTableItem, String> removeColumn; // Column for the remove button

    @FXML
    private Label totalPriceLabel; // Label to display total price

    private ShoppingCart shoppingCart; // Holds the shopping cart data

    private final CartService cartService = CartService.getInstance(); // Cart service for managing cart operations
    private final IInventoryService inventoryService = ServiceManager.getInstance().getInventoryService(); // Inventory
                                                                                                           // service

    private final SimpleDoubleProperty totalPrice = new SimpleDoubleProperty(0.0); // Binds the total price to the UI
    private final Map<Book, Integer> reservedStock = new HashMap<>(); // Holds reserved stock for items during checkout

    /**
     * Initializes the shopping cart UI components and bindings.
     * Loads the custom CSS for styling the TableView and buttons.
     */
    @FXML
    public void initialize() {
        // Add listener to load stylesheet once the scene is set
        cartTableView.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                loadStylesheet();
                cartTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }
        });

        // Configure item selection checkbox in the table
        selectColumn.setCellFactory(_ -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(Boolean isSelected, boolean empty) {
                super.updateItem(isSelected, empty);
                if (empty) {
                    setGraphic(null); // Clear the graphic if the cell is empty
                } else {
                    CartTableItem cartItem = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(cartItem.isSelected());
                    checkBox.selectedProperty().addListener((_, _, newValue) -> {
                        cartItem.setSelected(newValue); // Update selection state
                        updateTotalPrice(); // Recalculate total price when selection changes
                    });
                    setGraphic(checkBox); // Set the checkbox in the table cell
                }
            }
        });

        // Bind item name and total price columns
        itemNameColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
        totalAmountColumn.setCellValueFactory(cellData -> cellData.getValue().totalAmountProperty().asObject());

        // Configure quantity adjustment buttons (increment and decrement)
        quantityColumn.setCellFactory(_ -> new TableCell<>() {
            private final Button incrementButton = new Button("+");
            private final Button decrementButton = new Button("-");
            private final Label quantityLabel = new Label();
            private final HBox container = new HBox(10, decrementButton, quantityLabel, incrementButton);

            {
                container.setStyle("-fx-alignment: center;");
                container.setSpacing(10);
            }

            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CartTableItem cartItem = getTableView().getItems().get(getIndex());

                    // BIND instead of setText
                    quantityLabel.textProperty().unbind();
                    quantityLabel.textProperty().bind(
                            cartItem.quantityProperty().asString());

                    setGraphic(container);

                    // Add listeners for increment and decrement buttons
                    decrementButton.setOnAction(_ -> adjustQuantity(cartItem, -1));
                    incrementButton.setOnAction(_ -> adjustQuantity(cartItem, 1));

                }
            }
        });

        // Configure the remove button for each item
        removeColumn.setCellValueFactory(_ -> new SimpleStringProperty("Remove"));
        removeColumn.setCellFactory(_ -> new TableCell<>() {
            private final Button removeButton = new Button("Remove");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null); // Clear the graphic if the cell is empty
                } else {
                    setGraphic(removeButton); // Set the remove button in the cell
                    removeButton.setOnAction(_ -> {
                        CartTableItem cartItem = getTableView().getItems().get(getIndex());
                        removeItem(cartItem); // Remove the item when the button is clicked
                    });
                }
            }
        });

        // Bind total price label to the calculated total price
        totalPriceLabel.textProperty().bind(totalPrice.asString("Total Price: $%.2f"));
    }

    /**
     * Loads the external CSS stylesheet for styling the shopping cart UI.
     */
    private void loadStylesheet() {
        Parent root = cartTableView.getScene().getRoot();
        UIUtils.loadCSS(root, "/com/beyourshelf/css/table-style.css");
    }

    /**
     * Sets the shopping cart data and loads the cart items into the UI.
     *
     * @param shoppingCart The shopping cart associated with the user.
     */
    public void setShoppingCart(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart;
        System.out.println("Setting shopping cart with ID: " + shoppingCart.getCartId());
        loadCartItems(); // Load items from the shopping cart
    }

    /**
     * Loads the cart items from the shopping cart into the TableView.
     */
    private void loadCartItems() {
        List<CartTableItem> cartItems = shoppingCart.getBooks().entrySet().stream()
                .map(entry -> new CartTableItem(entry.getKey(), entry.getValue())) // Convert each book and its quantity
                                                                                   // into a CartTableItem
                .collect(Collectors.toList()); // Collect the items into a list

        cartTableView.getItems().setAll(cartItems); // Set items in the TableView
        updateTotalPrice(); // Update the total price
    }

    /**
     * Recalculates and updates the total price of the selected items.
     */
    private void updateTotalPrice() {
        double total = cartTableView.getItems().stream()
                .filter(CartTableItem::isSelected) // Calculate total for only selected items
                .mapToDouble(CartTableItem::getTotalAmount)
                .sum();
        totalPrice.set(total); // Update the bound total price
    }

    /**
     * Handles the checkout process by confirming and reserving stock for selected
     * items.
     */
    @FXML
    public void handleCheckout() {
        List<CartTableItem> selectedItems = cartTableView.getItems().stream()
                .filter(CartTableItem::isSelected) // Only proceed with selected items
                .toList(); // Collect items into a list

        if (selectedItems.isEmpty()) {
            UIUtils.showAlert("Checkout Error", "No items selected for checkout.");
            return;
        }

        // Confirm checkout with the user
        boolean confirmed = UIUtils.showConfirmation("Confirm Checkout", "Are you sure you want to checkout?");
        if (confirmed) {
            reserveStockInMemory(selectedItems); // Reserve stock for selected items
            loadPaymentScreen(selectedItems.stream().mapToDouble(CartTableItem::getTotalAmount).sum()); // Load payment
                                                                                                        // screen with
                                                                                                        // total amount
        }
    }

    /**
     * Reserves stock for the selected items in memory before proceeding with
     * payment.
     *
     * @param selectedItems The selected items to reserve stock for.
     */
    private void reserveStockInMemory(List<CartTableItem> selectedItems) {
        selectedItems.forEach(item -> reservedStock.put(item.getBook(), item.getQuantity())); // Reserve stock in memory
    }

    /**
     * Loads the payment screen for completing the purchase.
     *
     * @param totalAmount The total amount to be paid for the selected items.
     */
    private void loadPaymentScreen(double totalAmount) {
        Stage paymentStage = new Stage();
        UIUtils.loadModal("/com/beyourshelf/fxml/user/payment.fxml", "Payment", controller -> {
            PaymentController paymentController = (PaymentController) controller;
            int userId = SessionManager.getInstance().getUserId();
            paymentController.setPaymentDetails(totalAmount, userId, shoppingCart, this); // Pass payment details to the
                                                                                          // PaymentController
        }, paymentStage);
    }

    /**
     * Finalizes the stock adjustments and clears the cart after a successful
     * payment.
     */
    public void finalizeStockAfterPayment() {
        inventoryService.finalizeStockAdjustments(reservedStock); // Finalize stock in the inventory
        reservedStock.clear(); // Clear reserved stock in memory

        // Reset the cart by removing checked-out items
        removeCheckedOutItemsFromCart();
        updateTotalPrice(); // Recalculate the total price
    }

    /**
     * Removes items from the cart that were selected and checked out.
     */
    protected void removeCheckedOutItemsFromCart() {
        List<CartTableItem> selectedItems = cartTableView.getItems().stream()
                .filter(CartTableItem::isSelected)
                .toList(); // Collect selected items

        // Collect the books to remove from the cart
        List<Book> booksToRemove = selectedItems.stream()
                .map(CartTableItem::getBook)
                .toList(); // Collect the books

        // Remove books from the cart via the service
        cartService.removeBooksFromCart(shoppingCart.getCartId(), booksToRemove);
        shoppingCart.removeBooks(booksToRemove); // Remove books from in-memory cart
        cartTableView.getItems().removeAll(selectedItems); // Remove items from the UI
        updateTotalPrice(); // Recalculate total price
    }

    /**
     * Checks if a specific book is selected for checkout.
     *
     * @param book The book to check.
     * @return true if the book is selected, false otherwise.
     */
    public boolean isBookSelected(Book book) {
        return cartTableView.getItems().stream()
                .filter(cartItem -> cartItem.getBook().equals(book))
                .anyMatch(CartTableItem::isSelected); // Check if the book is selected
    }

    /**
     * Reverts any stock that was reserved in memory in case of payment failure or
     * cancellation.
     */
    public void revertReservedStock() {
        reservedStock.clear(); // Clear the reserved stock in memory
    }

    /**
     * Removes a specific item from both the UI and the shopping cart.
     *
     * @param cartItem The item to remove.
     */
    private void removeItem(CartTableItem cartItem) {
        System.out.println("Removing book from cart - Cart ID: " + shoppingCart.getCartId() + ", Book ID: "
                + cartItem.getBook().getBookId());
        shoppingCart.removeBook(cartItem.getBook()); // Remove from the cart model in memory
        cartService.removeBookFromCart(shoppingCart.getCartId(), cartItem.getBook().getBookId()); // Remove from the
                                                                                                  // database
        cartTableView.getItems().remove(cartItem); // Remove the item from the table view
        updateTotalPrice(); // Update the total price
    }

    /**
     * Adjusts the quantity of a specific item in the cart.
     *
     * @param cartItem   The cart item to adjust.
     * @param adjustment The quantity adjustment (positive for increment, negative
     *                   for decrement).
     */
    private void adjustQuantity(CartTableItem cartItem, int adjustment) {
        int newQuantity = cartItem.getQuantity() + adjustment;
        if (newQuantity > 0) {
            cartItem.setQuantity(newQuantity);
            shoppingCart.updateBookQuantity(cartItem.getBook(), newQuantity);
            cartService.updateBookQuantity(shoppingCart.getCartId(), cartItem.getBook().getBookId(), newQuantity);
            updateTotalPrice();
        } else {
            // Quantity reached zero or below: remove the item from cart and DB
            removeItem(cartItem);
        }
    }
}
