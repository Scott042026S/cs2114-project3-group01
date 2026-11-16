import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * One restaurant order.
 *
 * <p>An order has one or more dishes and can have notes.
 *
 * <p>Every order starts as WAITING. The kitchen can start it and then it gets
 * served, or it can be cancelled before the kitchen starts it.
 *
 * @author Archer Kim
 * @version 11/16/2026
 */
public class Order {

    /** Highest table number in the restaurant. */
    public static final int MAX_TABLES = 50;

    /** Longest notes allowed. */
    public static final int MAX_DESCRIPTION_LENGTH = 100;

    /** Message shown for a bad table number. */
    public static final String INVALID_TABLE_MESSAGE =
        "Enter a table number from 1 to " + MAX_TABLES;

    /** Message shown for bad dishes. */
    public static final String INVALID_ITEMS_MESSAGE =
        "Add at least one dish";

    /** Message shown for bad notes. */
    public static final String INVALID_DESCRIPTION_MESSAGE =
        "Notes can be up to " + MAX_DESCRIPTION_LENGTH
            + " letters, numbers, spaces, and , . ' - & ( ) /";

    /** Characters allowed in notes. */
    private static final Pattern ALLOWED_DESCRIPTION =
        Pattern.compile("[\\p{L}\\p{N} ,.'&()/\\-]+");

    private final int orderId;
    private final int tableNumber;
    private final Map<String, Integer> items;
    private final String description;
    private final boolean rush;
    private boolean kitchenStarted;
    private OrderStatus status;

    /**
     * Creates a new order.
     *
     * @param orderId     the order's ID (1 or more)
     * @param tableNumber the table number (1 to 50)
     * @param items       the dishes and how many of each
     * @param notes       extra notes, can be blank
     * @param rush        true if this is a rush order
     * @throws IllegalArgumentException if any input is invalid
     */
    public Order(int orderId, int tableNumber, Map<String, Integer> items,
        String notes, boolean rush) {
        if (orderId < 1) {
            throw new IllegalArgumentException(
                "Order ID must be 1 or more, was " + orderId);
        }
        validate(tableNumber, items, notes);
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        // save a copy of the dishes that can't be changed
        this.items = Collections.unmodifiableMap(
            new LinkedHashMap<String, Integer>(items));
        this.description = cleanNotes(notes);
        this.rush = rush;
        this.kitchenStarted = false;
        this.status = OrderStatus.WAITING;
    }

    /**
     * Checks that a table number, dishes, and notes are valid.
     *
     * @param tableNumber the table number
     * @param items       the dishes and how many of each
     * @param notes       extra notes, can be blank
     * @throws IllegalArgumentException if anything is invalid
     */
    public static void validate(int tableNumber, Map<String, Integer> items,
        String notes) {
        if (tableNumber < 1 || tableNumber > MAX_TABLES) {
            throw new IllegalArgumentException(INVALID_TABLE_MESSAGE);
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(INVALID_ITEMS_MESSAGE);
        }
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            String dish = item.getKey();
            Integer quantity = item.getValue();
            if (dish == null || dish.trim().isEmpty()) {
                throw new IllegalArgumentException(INVALID_ITEMS_MESSAGE);
            }
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException(
                    "Quantity for " + dish + " must be 1 or more");
            }
        }
        String trimmed = cleanNotes(notes);
        if (!trimmed.isEmpty()
            && (trimmed.length() > MAX_DESCRIPTION_LENGTH
                || !ALLOWED_DESCRIPTION.matcher(trimmed).matches())) {
            throw new IllegalArgumentException(INVALID_DESCRIPTION_MESSAGE);
        }
    }

    /**
     * Checks that this order is valid.
     *
     * @throws IllegalArgumentException if anything is invalid
     */
    public void validate() {
        validate(tableNumber, items, description);
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
     * Gets the dishes on this order.
     *
     * @return the dishes and how many of each
     */
    public Map<String, Integer> getItems() {
        return items;
    }

    /**
     * Gets how many of one dish are on this order.
     *
     * @param dish the dish name
     * @return how many, or 0 if it's not on the order
     */
    public int getQuantity(String dish) {
        Integer quantity = items.get(dish);
        return (quantity == null) ? 0 : quantity;
    }

    /**
     * Gets the notes.
     *
     * @return the notes, or "" if there are none
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
     * Trims the notes. Null becomes "".
     *
     * @param notes the notes
     * @return the trimmed notes
     */
    private static String cleanNotes(String notes) {
        return (notes == null) ? "" : notes.trim();
    }

    /**
     * Returns the order as text, for debugging.
     *
     * @return the order as text
     */
    @Override
    public String toString() {
        return "#" + orderId + " table " + tableNumber
            + (rush ? " RUSH" : "") + " " + items
            + (description.isEmpty() ? "" : " \"" + description + "\"")
            + " " + status
            + (kitchenStarted && status == OrderStatus.WAITING
                ? " (cooking)" : "");
    }
}
