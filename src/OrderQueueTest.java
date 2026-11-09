import java.util.Arrays;
import java.util.List;

import student.TestCase;

/**
 * Tests the OrderQueue model independently of the user interface.
 */
public class OrderQueueTest extends TestCase
{
    private OrderQueue queue;

    public void setUp()
    {
        queue = new OrderQueue();
    }

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
    public void testRushPriorityAndFifoWithinEachGroup()
    {
        Order normal1 = queue.enqueue(1, "Soup", false);
        Order rush1 = queue.enqueue(2, "Salad", true);
        Order normal2 = queue.enqueue(3, "Pasta", false);
        Order rush2 = queue.enqueue(4, "Fries", true);

        assertEquals(Arrays.asList(rush1, rush2, normal1, normal2),
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

    public void testCookingOrderStaysFirst()
    {
        Order cooking = queue.enqueue(1, "Soup", false);
        assertSame(cooking, queue.startNext());
        assertTrue(cooking.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, cooking.getStatus());
        assertEquals(1, queue.size());

        Order rush = queue.enqueue(2, "Salad", true);
        assertSame(cooking, queue.peek());
        assertEquals(Arrays.asList(cooking, rush), queue.getOrdersInServeOrder());
        expectException(IllegalStateException.class, queue::startNext);
        assertFalse(rush.isKitchenStarted());
        assertSame(cooking, queue.serveNext());
        assertSame(rush, queue.peek());
    }

    public void testCancelByNewestIdAcrossBothGroups()
    {
        Order normal1 = queue.enqueue(1, "Soup", false);
        Order rush = queue.enqueue(2, "Salad", true);
        Order normal2 = queue.enqueue(3, "Pasta", false);

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

    public void testCancelRefusesNewestIfCooking()
    {
        Order older = queue.enqueue(1, "Soup", false);
        Order newest = queue.enqueue(2, "Salad", true);
        assertSame(newest, queue.startNext());

        expectException(IllegalStateException.class, queue::cancelLast);
        assertEquals(Arrays.asList(newest, older), queue.getOrdersInServeOrder());
        assertEquals(OrderStatus.WAITING, newest.getStatus());
        assertEquals(OrderStatus.WAITING, older.getStatus());
    }

    public void testRejectedEnqueuePreservesStateAndId()
    {
        Order first = queue.enqueue(1, "Chicken  soup", false);
        assertTrue(queue.hasWaitingOrder(1, " CHICKEN soup "));
        assertFalse(queue.hasWaitingOrder(2, "Chicken soup"));
        assertFalse(queue.hasWaitingOrder(1, null));
        expectException(IllegalStateException.class,
            () -> queue.enqueue(1, " CHICKEN soup ", true));
        expectException(IllegalArgumentException.class, () -> queue.enqueue(0, "Soup", false));
        expectException(IllegalArgumentException.class, () -> queue.enqueue(2, " ", false));
        assertEquals(1, queue.size());
        assertSame(first, queue.peek());
        assertEquals(2, queue.getNextOrderId());

        assertSame(first, queue.dequeue());
        assertFalse(queue.hasWaitingOrder(1, "Chicken soup"));
        assertEquals(2, queue.enqueue(1, "Chicken soup", true).getOrderId());
    }

    public void testAddExistingOrderChecksEligibility()
    {
        Order first = new Order(queue.getNextOrderId(), 1, "Soup", false);
        assertSame(first, queue.addOrder(first));
        expectException(IllegalArgumentException.class, () -> queue.addOrder(null));
        expectException(IllegalArgumentException.class,
            () -> queue.addOrder(new Order(1, 2, "Salad", true)));
        Order started = new Order(2, 2, "Salad", true);
        started.setKitchenStarted(true);
        expectException(IllegalArgumentException.class, () -> queue.addOrder(started));
        expectException(IllegalStateException.class,
            () -> queue.addOrder(new Order(2, 1, "soup", true)));
        assertEquals(1, queue.size());
        assertEquals(2, queue.getNextOrderId());
        assertSame(first, queue.peek());
    }

    public void testServeOrderIsAnUnmodifiableSnapshot()
    {
        Order first = queue.enqueue(1, "Soup", false);
        List<Order> snapshot = queue.getOrdersInServeOrder();
        expectException(UnsupportedOperationException.class, snapshot::clear);
        Order rush = queue.enqueue(2, "Salad", true);
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
