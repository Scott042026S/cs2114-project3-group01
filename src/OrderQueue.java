import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * The line of orders for the kitchen.
 *
 * <p>Rush orders and normal orders are kept in two separate deques. The next
 * order served is the one being cooked, then the oldest rush order, then the
 * oldest normal order.
 *
 * <p>The method names from the system diagram (addOrder, serveNext,
 * getNextOrder, getWaitingCount, getQueue) and from the written spec
 * (enqueue, dequeue, peek, size, getOrdersInServeOrder) both work.
 *
 * @author Archer Kim
 * @version 11/08/2026
 */
public class OrderQueue {

    private final Deque<Order> rushOrders;
    private final Deque<Order> normalOrders;
    private int nextOrderId;

    /**
     * Creates an empty queue.
     */
    public OrderQueue() {
        rushOrders = new ArrayDeque<Order>();
        normalOrders = new ArrayDeque<Order>();
        nextOrderId = 1;
    }

    /**
     * Adds a new order to the back of the rush or normal line.
     *
     * @param tableNumber the table number
     * @param description what was ordered
     * @param rush        true for a rush order
     * @return the new order
     * @throws IllegalArgumentException if the table number or description is
     *                                  invalid
     * @throws IllegalStateException    if the table already has the same
     *                                  order waiting
     */
    public Order enqueue(int tableNumber, String description, boolean rush) {
        Order.validate(tableNumber, description);
        if (hasWaitingOrder(tableNumber, description)) {
            throw new IllegalStateException(
                "Table " + tableNumber + " already has that order waiting");
        }
        Order order = new Order(nextOrderId, tableNumber, description, rush);
        nextOrderId++;
        addToLine(order);
        return order;
    }

    /**
     * Adds an order that was already created. Give it the next ID, like
     * {@code new Order(queue.getNextOrderId(), table, description, rush)}.
     *
     * @param order the new order
     * @return the order that was added
     * @throws IllegalArgumentException if the order is null, is not new, or
     *                                  uses an ID that was already used
     * @throws IllegalStateException    if the table already has the same
     *                                  order waiting
     */
    public Order addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getStatus() != OrderStatus.WAITING
            || order.isKitchenStarted()) {
            throw new IllegalArgumentException("Only new orders can be added");
        }
        if (order.getOrderId() < nextOrderId) {
            throw new IllegalArgumentException(
                "Order ID " + order.getOrderId() + " was already used");
        }
        if (hasWaitingOrder(order.getTableNumber(), order.getDescription())) {
            throw new IllegalStateException("Table " + order.getTableNumber()
                + " already has that order waiting");
        }
        nextOrderId = order.getOrderId() + 1;
        addToLine(order);
        return order;
    }

    /**
     * Gets the ID the next new order should use.
     *
     * @return the next order ID
     */
    public int getNextOrderId() {
        return nextOrderId;
    }

    /**
     * Removes the next order and marks it as served.
     *
     * @return the served order
     * @throws IllegalStateException if the queue is empty
     */
    public Order dequeue() {
        Deque<Order> source = dequeHoldingNext();
        if (source == null) {
            throw new IllegalStateException("No orders to serve");
        }
        Order order = source.pollFirst();
        order.markServed();
        return order;
    }

    /**
     * Gets the next order without removing it.
     *
     * @return the next order, or null if the queue is empty
     */
    public Order peek() {
        Deque<Order> source = dequeHoldingNext();
        if (source == null) {
            return null;
        }
        return source.peekFirst();
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if there are no orders
     */
    public boolean isEmpty() {
        return rushOrders.isEmpty() && normalOrders.isEmpty();
    }

    /**
     * Gets the number of orders in the queue.
     *
     * @return the number of orders
     */
    public int size() {
        return rushOrders.size() + normalOrders.size();
    }

    /**
     * Marks the next order as being cooked. It stays in the queue.
     *
     * @return the started order
     * @throws IllegalStateException if the queue is empty or the next order
     *                               is already cooking
     */
    public Order startNext() {
        Order next = peek();
        if (next == null) {
            throw new IllegalStateException("No orders to start");
        }
        if (next.isKitchenStarted()) {
            throw new IllegalStateException("That order is already cooking");
        }
        next.markKitchenStarted();
        return next;
    }

    /**
     * Cancels the most recently added order.
     *
     * @return the cancelled order
     * @throws IllegalStateException if the queue is empty or the kitchen
     *                               already started that order
     */
    public Order cancelLast() {
        Deque<Order> source = dequeHoldingNewest();
        if (source == null) {
            throw new IllegalStateException("No orders to cancel");
        }
        Order newest = source.peekLast();
        if (newest.isKitchenStarted()) {
            throw new IllegalStateException(
                "The kitchen has already started the order");
        }
        source.pollLast();
        newest.markCancelled();
        return newest;
    }

    /**
     * Checks if a table already has the same order in the queue. Capital
     * letters and extra spaces are ignored.
     *
     * @param tableNumber the table number
     * @param description what was ordered
     * @return true if a matching order is found
     */
    public boolean hasWaitingOrder(int tableNumber, String description) {
        if (description == null) {
            return false;
        }
        String target = normalize(description);
        return containsMatch(rushOrders, tableNumber, target)
            || containsMatch(normalOrders, tableNumber, target);
    }

    /**
     * Gets all orders in the order they will be served.
     *
     * @return a list of the orders that cannot be changed
     */
    public List<Order> getOrdersInServeOrder() {
        List<Order> result = new ArrayList<Order>(size());
        Order first = peek();
        Order started = null;
        if (first != null && first.isKitchenStarted()) {
            started = first;
            result.add(started);
        }
        addAllExcept(result, rushOrders, started);
        addAllExcept(result, normalOrders, started);
        return Collections.unmodifiableList(result);
    }

    // ----- Names from the system diagram. Each does the same thing as the
    // ----- method it calls.

    /**
     * Removes the next order and marks it as served. Same as dequeue().
     *
     * @return the served order
     * @throws IllegalStateException if the queue is empty
     */
    public Order serveNext() {
        return dequeue();
    }

    /**
     * Gets the next order without removing it. Same as peek().
     *
     * @return the next order, or null if the queue is empty
     */
    public Order getNextOrder() {
        return peek();
    }

    /**
     * Gets the number of orders in the queue. Same as size().
     *
     * @return the number of orders
     */
    public int getWaitingCount() {
        return size();
    }

    /**
     * Gets all orders in the order they will be served. Same as
     * getOrdersInServeOrder().
     *
     * @return a list of the orders that cannot be changed
     */
    public List<Order> getQueue() {
        return getOrdersInServeOrder();
    }

    /**
     * Returns the queue as text, for debugging.
     *
     * @return the queue as text
     */
    @Override
    public String toString() {
        return getOrdersInServeOrder().toString();
    }

    /**
     * Puts an order at the back of the rush or normal line.
     *
     * @param order the order to add
     */
    private void addToLine(Order order) {
        if (order.isRush()) {
            rushOrders.offerLast(order);
        }
        else {
            normalOrders.offerLast(order);
        }
    }

    /**
     * Finds the deque with the next order at its front. The order being
     * cooked is always at the front of its deque.
     *
     * @return the deque, or null if both are empty
     */
    private Deque<Order> dequeHoldingNext() {
        if (isStarted(rushOrders.peekFirst())) {
            return rushOrders;
        }
        if (isStarted(normalOrders.peekFirst())) {
            return normalOrders;
        }
        if (!rushOrders.isEmpty()) {
            return rushOrders;
        }
        if (!normalOrders.isEmpty()) {
            return normalOrders;
        }
        return null;
    }

    /**
     * Finds the deque with the newest order at its back.
     *
     * @return the deque, or null if both are empty
     */
    private Deque<Order> dequeHoldingNewest() {
        Order lastRush = rushOrders.peekLast();
        Order lastNormal = normalOrders.peekLast();
        if (lastRush == null && lastNormal == null) {
            return null;
        }
        if (lastRush == null) {
            return normalOrders;
        }
        if (lastNormal == null) {
            return rushOrders;
        }
        if (lastRush.getOrderId() > lastNormal.getOrderId()) {
            return rushOrders;
        }
        return normalOrders;
    }

    /**
     * Checks if an order exists and is being cooked.
     *
     * @param order the order, which may be null
     * @return true if it is being cooked
     */
    private static boolean isStarted(Order order) {
        return order != null && order.isKitchenStarted();
    }

    /**
     * Checks if a deque has an order with this table and description.
     *
     * @param orders      the deque to search
     * @param tableNumber the table number
     * @param target      the cleaned-up description
     * @return true if a match is found
     */
    private static boolean containsMatch(Deque<Order> orders, int tableNumber,
        String target) {
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber
                && normalize(order.getDescription()).equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds every order in a deque to a list, except one.
     *
     * @param result the list to add to
     * @param orders the deque to copy from
     * @param skip   the order to leave out, or null
     */
    private static void addAllExcept(List<Order> result, Deque<Order> orders,
        Order skip) {
        for (Order order : orders) {
            if (order != skip) {
                result.add(order);
            }
        }
    }

    /**
     * Cleans up a description so it can be compared: trims it, removes
     * extra spaces, and makes it lowercase.
     *
     * @param text the description
     * @return the cleaned-up description
     */
    private static String normalize(String text) {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
