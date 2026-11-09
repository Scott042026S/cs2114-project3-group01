import student.TestCase;

/**
 * Tests the Order model independently of the user interface.
 */
public class OrderTest extends TestCase
{
    public void testConstructAndTrim()
    {
        Order order = new Order(7, 1, "  2 burgers, fries  ", true);
        assertEquals(7, order.getOrderId());
        assertEquals(1, order.getTableNumber());
        assertEquals("2 burgers, fries", order.getDescription());
        assertTrue(order.isRush());
        assertFalse(order.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, order.getStatus());
        order.validate();
    }

    public void testValidationBoundaries()
    {
        String longest = "a".repeat(Order.MAX_DESCRIPTION_LENGTH);
        Order order = new Order(1, Order.MAX_TABLES, " " + longest + " ", false);
        assertEquals(longest, order.getDescription());
        assertEquals(Order.MAX_TABLES, order.getTableNumber());
        assertFalse(order.isRush());
        expectException(IllegalArgumentException.class, () -> new Order(0, 1, "Soup", false));
        expectException(IllegalArgumentException.class, () -> Order.validate(0, "Soup"));
        expectException(IllegalArgumentException.class,
            () -> Order.validate(Order.MAX_TABLES + 1, "Soup"));
        expectException(IllegalArgumentException.class, () -> Order.validate(1, longest + "a"));
    }

    public void testInvalidDescriptions()
    {
        expectException(IllegalArgumentException.class, () -> Order.validate(1, null));
        expectException(IllegalArgumentException.class, () -> Order.validate(1, "   "));
        expectException(IllegalArgumentException.class, () -> Order.validate(1, "Soup!"));
    }

    public void testStartAndServe()
    {
        Order order = new Order(1, 1, "Soup", false);
        order.setKitchenStarted(true);
        assertTrue(order.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, order.getStatus());
        expectException(IllegalStateException.class, () -> order.setKitchenStarted(true));
        expectException(IllegalStateException.class, () -> order.setKitchenStarted(false));
        expectException(IllegalStateException.class, () -> order.setStatus(OrderStatus.CANCELLED));
        assertTrue(order.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, order.getStatus());
        order.setStatus(OrderStatus.SERVED);
        assertEquals(OrderStatus.SERVED, order.getStatus());
        expectException(IllegalStateException.class, () -> order.setStatus(OrderStatus.WAITING));
    }

    public void testCancelBeforeCooking()
    {
        Order order = new Order(1, 1, "Soup", false);
        expectException(IllegalArgumentException.class, () -> order.setStatus(null));
        assertEquals(OrderStatus.WAITING, order.getStatus());
        order.setStatus(OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertFalse(order.isKitchenStarted());
        expectException(IllegalStateException.class, () -> order.setKitchenStarted(true));
        expectException(IllegalStateException.class, () -> order.setStatus(OrderStatus.SERVED));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
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
        assertTrue("Expected " + expected.getSimpleName() + " but caught "
            + caught.getClass().getSimpleName(), expected.isInstance(caught));
    }
}
