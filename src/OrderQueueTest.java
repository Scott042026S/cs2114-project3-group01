import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import student.TestCase;

/**
 * Tests the OrderQueue model independently of the user interface.
 *
 * @author Archer Kim
 * @version 11/16/2026
 */
public class OrderQueueTest
    extends TestCase
{
    private OrderQueue queue;

    /**
     * Creates an empty queue before each test.
     */
    public void setUp()
    {
        queue = new OrderQueue();
    }


    /**
     * Builds a one-dish map with a quantity of 1.
     */
    private static Map<String, Integer> one(String dish)
    {
        Map<String, Integer> items = new LinkedHashMap<String, Integer>();
        items.put(dish, 1);
        return items;
    }


    /**
     * Adds a one-dish order with no notes.
     */
    private Order add(int table, String dish, boolean rush)
    {
        return queue.enqueue(table, one(dish), "", rush);
    }


    /**
     * An empty queue reports nothing and refuses every operation.
     */
    public void testEmptyQueue()
    {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.getWaitingCount());
        assertNull(queue.getNextOrder());
        assertTrue(queue.getQueue().isEmpty());
        expectException(IllegalStateException.class, queue::serveNext);
        expectException(IllegalStateException.class, queue::startNext);
        expectException(IllegalStateException.class, queue::cancelLast);
        assertTrue(queue.isEmpty());
        assertEquals(1, queue.getNextOrderId());
    }


    /**
     * Rush orders go first, and each group is first in, first out.
     */
    public void testRushPriorityAndFifoWithinEachGroup()
    {
        Order normal1 = add(1, "Soup", false);
        Order rush1 = add(2, "Salad", true);
        Order normal2 = add(3, "Pasta", false);
        Order rush2 = add(4, "Fries", true);

        assertEquals(
            Arrays.asList(rush1, rush2, normal1, normal2),
            queue.getOrdersInServeOrder());
        assertSame(rush1, queue.peek());
        assertEquals(4, queue.size());
        assertSame(rush1, queue.dequeue());
        assertEquals(OrderStatus.SERVED, rush1.getStatus());
        assertTrue(rush1.isKitchenStarted());
        assertEquals(3, queue.size());
        assertSame(rush2, queue.dequeue());
        assertSame(normal1, queue.dequeue());
        assertSame(normal2, queue.dequeue());
        assertTrue(queue.isEmpty());
    }


    /**
     * A cooking order stays at the front even when a rush order arrives.
     */
    public void testCookingOrderStaysFirst()
    {
        Order cooking = add(1, "Soup", false);
        assertSame(cooking, queue.startNext());
        assertTrue(cooking.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, cooking.getStatus());
        assertEquals(1, queue.size());

        Order rush = add(2, "Salad", true);
        assertSame(cooking, queue.peek());
        assertEquals(
            Arrays.asList(cooking, rush),
            queue.getOrdersInServeOrder());
        expectException(IllegalStateException.class, queue::startNext);
        assertFalse(rush.isKitchenStarted());
        assertSame(cooking, queue.serveNext());
        assertSame(rush, queue.peek());
    }


    /**
     * cancelLast removes the newest order by ID, whichever line it is in.
     */
    public void testCancelByNewestIdAcrossBothGroups()
    {
        Order normal1 = add(1, "Soup", false);
        Order rush = add(2, "Salad", true);
        Order normal2 = add(3, "Pasta", false);

        assertSame(normal2, queue.cancelLast());
        assertEquals(OrderStatus.CANCELLED, normal2.getStatus());
        assertSame(rush, queue.cancelLast());
        assertEquals(OrderStatus.CANCELLED, rush.getStatus());
        assertSame(normal1, queue.peek());
        assertEquals(1, queue.size());
        assertSame(normal1, queue.cancelLast());
        assertTrue(queue.isEmpty());
        assertEquals(4, queue.getNextOrderId());
    }


    /**
     * The newest order can't be cancelled once the kitchen has started it.
     */
    public void testCancelRefusesNewestIfCooking()
    {
        Order older = add(1, "Soup", false);
        Order newest = add(2, "Salad", true);
        assertSame(newest, queue.startNext());

        expectException(IllegalStateException.class, queue::cancelLast);
        assertEquals(
            Arrays.asList(newest, older),
            queue.getOrdersInServeOrder());
        assertEquals(OrderStatus.WAITING, newest.getStatus());
        assertEquals(OrderStatus.WAITING, older.getStatus());
    }


    /**
     * Duplicate and invalid orders are rejected without using up an ID.
     */
    public void testRejectedEnqueuePreservesStateAndId()
    {
        Order first = queue.enqueue(1, one("Soup"), "Extra  hot", false);
        assertTrue(queue.hasWaitingOrder(1, one("Soup"), " EXTRA hot "));
        assertFalse(queue.hasWaitingOrder(2, one("Soup"), "Extra hot"));
        assertFalse(queue.hasWaitingOrder(1, one("Soup"), ""));
        assertFalse(queue.hasWaitingOrder(1, one("Salad"), "Extra hot"));
        assertFalse(queue.hasWaitingOrder(1, null, "Extra hot"));
        expectException(
            IllegalStateException.class,
            () -> queue.enqueue(1, one("Soup"), " EXTRA hot ", true));
        expectException(
            IllegalArgumentException.class,
            () -> queue.enqueue(0, one("Soup"), "", false));
        expectException(
            IllegalArgumentException.class,
            () -> queue.enqueue(2, one(" "), "", false));
        expectException(
            IllegalArgumentException.class,
            () -> queue
                .enqueue(2, new LinkedHashMap<String, Integer>(), "", false));
        expectException(
            IllegalArgumentException.class,
            () -> queue.enqueue(2, one("Soup"), "Soup!", false));
        assertEquals(1, queue.size());
        assertSame(first, queue.peek());
        assertEquals(2, queue.getNextOrderId());

        assertSame(first, queue.dequeue());
        assertFalse(queue.hasWaitingOrder(1, one("Soup"), "Extra hot"));
        assertEquals(
            2,
            queue.enqueue(1, one("Soup"), "Extra hot", true).getOrderId());
    }


    /**
     * addOrder only takes new orders with unused IDs that aren't duplicates.
     */
    public void testAddExistingOrderChecksEligibility()
    {
        Order first =
            new Order(queue.getNextOrderId(), 1, one("Soup"), "", false);
        assertSame(first, queue.addOrder(first));
        expectException(
            IllegalArgumentException.class,
            () -> queue.addOrder(null));
        expectException(
            IllegalArgumentException.class,
            () -> queue.addOrder(new Order(1, 2, one("Salad"), "", true)));
        Order started = new Order(2, 2, one("Salad"), "", true);
        started.setKitchenStarted(true);
        expectException(
            IllegalArgumentException.class,
            () -> queue.addOrder(started));
        expectException(
            IllegalStateException.class,
            () -> queue.addOrder(new Order(2, 1, one("Soup"), "", true)));
        assertEquals(1, queue.size());
        assertEquals(2, queue.getNextOrderId());
        assertSame(first, queue.peek());
    }


    /**
     * The serve-order list is a snapshot that can't be changed.
     */
    public void testServeOrderIsAnUnmodifiableSnapshot()
    {
        Order first = add(1, "Soup", false);
        List<Order> snapshot = queue.getOrdersInServeOrder();
        expectException(UnsupportedOperationException.class, snapshot::clear);
        Order rush = add(2, "Salad", true);
        assertEquals(Arrays.asList(first), snapshot);
        assertEquals(Arrays.asList(rush, first), queue.getQueue());
    }


    /**
     * Runs one operation and checks that it throws the expected exception.
     */
    private void expectException(
        Class<? extends RuntimeException> expected,
        Runnable operation)
    {
        RuntimeException caught = null;
        try
        {
            operation.run();
        }
        catch (RuntimeException exception)
        {
            caught = exception;
        }
        assertNotNull("Expected " + expected.getSimpleName(), caught);
        assertTrue(
            "Expected " + expected.getSimpleName() + " but caught "
                + caught.getClass().getSimpleName(),
            expected.isInstance(caught));
    }
}
