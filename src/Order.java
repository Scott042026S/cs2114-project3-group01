import java.util.regex.Pattern;

/**
 * One restaurant order.
 *
 * <p>Every order starts as WAITING. The kitchen can start it and then it gets
 * served, or it can be cancelled before the kitchen starts it.
 *
 * @author Archer Kim
 * @version 11/08/2026
 */
public class Order {

    /** Highest table number in the restaurant. */
    public static final int MAX_TABLES = 50;

    /** Longest description allowed. */
    public static final int MAX_DESCRIPTION_LENGTH = 100;

    /** Message shown for a bad table number. */
    public static final String INVALID_TABLE_MESSAGE =
        "Enter a table number from 1 to " + MAX_TABLES;

    /** Message shown for a bad description. */
    public static final String INVALID_DESCRIPTION_MESSAGE =
        "Enter a valid order description";

    /**
     * Characters allowed in a description: letters, numbers, spaces, and
     * , . ' - &amp; ( ) /
     */
    private static final Pattern ALLOWED_DESCRIPTION =
        Pattern.compile("[\\p{L}\\p{N} ,.'&()/\\-]+");

    private final int orderId;
    private final int tableNumber;
    private final String description;
    private final boolean rush;
    private boolean kitchenStarted;
    private OrderStatus status;

    /**
     * Creates a new order.
     *
     * @param orderId     the order's ID (1 or more)
     * @param tableNumber the table number (1 to 50)
     * @param description what was ordered
     * @param rush        true if this is a rush order
     * @throws IllegalArgumentException if any input is invalid
     */
    public Order(int orderId, int tableNumber, String description,
        boolean rush) {
        if (orderId < 1) {
            throw new IllegalArgumentException(
                "Order ID must be 1 or more, was " + orderId);
        }
        validate(tableNumber, description);
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.description = description.trim();
        this.rush = rush;
        this.kitchenStarted = false;
        this.status = OrderStatus.WAITING;
    }

    /**
     * Checks that a table number and description are valid.
     *
     * @param tableNumber the table number to check
     * @param description the description to check
     * @throws IllegalArgumentException if either one is invalid
     */
    public static void validate(int tableNumber, String description) {
        if (tableNumber < 1 || tableNumber > MAX_TABLES) {
            throw new IllegalArgumentException(INVALID_TABLE_MESSAGE);
        }
        if (description == null) {
            throw new IllegalArgumentException(INVALID_DESCRIPTION_MESSAGE);
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()
            || trimmed.length() > MAX_DESCRIPTION_LENGTH
            || !ALLOWED_DESCRIPTION.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(INVALID_DESCRIPTION_MESSAGE);
        }
    }

    /**
     * Checks that this order's table number and description are valid.
     * This always passes, because the constructor already checked them.
     *
     * @throws IllegalArgumentException if either one is invalid
     */
    public void validate() {
        validate(tableNumber, description);
    }

    /**
     * Gets the order ID. A higher ID means the order was added later.
     *
     * @return the order ID
     */
    public int getOrderId() {
        return orderId;
    }

    /**
     * Gets the table number.
     *
     * @return the table number
     */
    public int getTableNumber() {
        return tableNumber;
    }

    /**
     * Gets what was ordered.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this is a rush order.
     *
     * @return true if it is a rush order
     */
    public boolean isRush() {
        return rush;
    }

    /**
     * Checks if the kitchen has started cooking this order.
     *
     * @return true if the kitchen has started it
     */
    public boolean isKitchenStarted() {
        return kitchenStarted;
    }

    /**
     * Gets the order's status.
     *
     * @return WAITING, SERVED, or CANCELLED
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Sets whether the kitchen has started this order. It can only change
     * from false to true, once. For an order in the queue, use
     * OrderQueue.startNext() instead so the line stays correct.
     *
     * @param started true to mark the order as being cooked
     * @throws IllegalStateException if the change is not allowed
     */
    public void setKitchenStarted(boolean started) {
        if (started) {
            markKitchenStarted();
        }
        else if (kitchenStarted) {
            throw new IllegalStateException(
                "A started order cannot be un-started");
        }
    }

    /**
     * Sets the order's status. The only changes allowed are WAITING to
     * SERVED, and WAITING to CANCELLED before the kitchen starts it. For an
     * order in the queue, use OrderQueue instead so the line stays correct.
     *
     * @param newStatus the new status
     * @throws IllegalArgumentException if newStatus is null
     * @throws IllegalStateException    if the change is not allowed
     */
    public void setStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (newStatus == OrderStatus.SERVED) {
            markServed();
        }
        else if (newStatus == OrderStatus.CANCELLED) {
            markCancelled();
        }
        else {
            requireWaiting(); // WAITING to WAITING is no change
        }
    }

    /**
     * Marks the order as being cooked.
     *
     * @throws IllegalStateException if it was already started, served, or
     *                               cancelled
     */
    void markKitchenStarted() {
        requireWaiting();
        if (kitchenStarted) {
            throw new IllegalStateException(
                "Order " + orderId + " has already started cooking");
        }
        kitchenStarted = true;
    }

    /**
     * Marks the order as served.
     *
     * @throws IllegalStateException if it was already served or cancelled
     */
    void markServed() {
        requireWaiting();
        kitchenStarted = true; // served food must have been cooked
        status = OrderStatus.SERVED;
    }

    /**
     * Marks the order as cancelled.
     *
     * @throws IllegalStateException if it was already started, served, or
     *                               cancelled
     */
    void markCancelled() {
        requireWaiting();
        if (kitchenStarted) {
            throw new IllegalStateException(
                "The kitchen has already started the order");
        }
        status = OrderStatus.CANCELLED;
    }

    /**
     * Makes sure the order is still waiting.
     *
     * @throws IllegalStateException if it was served or cancelled
     */
    private void requireWaiting() {
        if (status != OrderStatus.WAITING) {
            throw new IllegalStateException(
                "Order " + orderId + " is already " + status);
        }
    }

    /**
     * Returns the order as text, for debugging.
     *
     * @return the order as text
     */
    @Override
    public String toString() {
        return "#" + orderId + " table " + tableNumber
            + (rush ? " RUSH" : "") + " \"" + description + "\" " + status
            + (kitchenStarted && status == OrderStatus.WAITING
                ? " (cooking)" : "");
    }
}
