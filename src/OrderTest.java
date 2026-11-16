import java.util.LinkedHashMap;
import java.util.Map;

import student.TestCase;

/**
 * Tests the Order model independently of the user interface.
 *
 * @author Archer Kim
 * @version 11/16/2026
 */
public class OrderTest
    extends TestCase
{
    /**
     * Builds a dish map from name/quantity pairs, like items("Burger", 2,
     * "Fries", 1).
     */
    private static Map<String, Integer> items(Object... pairs)
    {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < pairs.length; i += 2)
        {
            result.put((String)pairs[i], (Integer)pairs[i + 1]);
        }
        return result;
    }


    /**
     * The constructor stores everything and trims the notes.
     */
    public void testConstructAndTrim()
    {
        Order order = new Order(
            7,
            1,
            items("Burger", 2, "Fries", 1),
            "  no onions  ",
            true);
        assertEquals(7, order.getOrderId());
        assertEquals(1, order.getTableNumber());
        assertEquals(items("Burger", 2, "Fries", 1), order.getItems());
        assertEquals(2, order.getQuantity("Burger"));
        assertEquals(0, order.getQuantity("Salad"));
        assertEquals("no onions", order.getDescription());
        assertTrue(order.isRush());
        assertFalse(order.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, order.getStatus());
        order.validate();
    }


    /**
     * Notes are optional: null and blank both become "".
     */
    public void testBlankNotesAllowed()
    {
        assertEquals(
            "",
            new Order(1, 1, items("Soup", 1), null, false).getDescription());
        assertEquals(
            "",
            new Order(2, 1, items("Soup", 1), "   ", false).getDescription());
    }


    /**
     * The order keeps its own copy of the dishes, and it can't be changed.
     */
    public void testItemsAreCopiedAndUnmodifiable()
    {
        Map<String, Integer> original = items("Soup", 1);
        Order order = new Order(1, 1, original, "", false);
        original.put("Salad", 3);
        assertEquals(items("Soup", 1), order.getItems());
        expectException(
            UnsupportedOperationException.class,
            () -> order.getItems().put("Fries", 1));
    }


    /**
     * Table numbers and note lengths at and past the limits.
     */
    public void testValidationBoundaries()
    {
        String longest = "a".repeat(Order.MAX_DESCRIPTION_LENGTH);
        Order order = new Order(
            1,
            Order.MAX_TABLES,
            items("Soup", 1),
            " " + longest + " ",
            false);
        assertEquals(longest, order.getDescription());
        assertEquals(Order.MAX_TABLES, order.getTableNumber());
        assertFalse(order.isRush());
        expectException(
            IllegalArgumentException.class,
            () -> new Order(0, 1, items("Soup", 1), "", false));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(0, items("Soup", 1), ""));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(Order.MAX_TABLES + 1, items("Soup", 1), ""));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, items("Soup", 1), longest + "a"));
    }


    /**
     * Missing dishes, blank dish names, and bad quantities are rejected.
     */
    public void testInvalidItems()
    {
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, null, ""));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, items(), ""));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, items("  ", 1), ""));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, items("Soup", 0), ""));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, items("Soup", null), ""));
    }


    /**
     * Notes with characters that aren't allowed are rejected.
     */
    public void testInvalidNotes()
    {
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, items("Soup", 1), "Soup!"));
        expectException(
            IllegalArgumentException.class,
            () -> Order.validate(1, items("Soup", 1), "<script>"));
        Order.validate(1, items("Soup", 1), "no onions, extra sauce (side)");
    }


    /**
     * A started order can be served but not cancelled or un-started.
     */
    public void testStartAndServe()
    {
        Order order = new Order(1, 1, items("Soup", 1), "", false);
        order.setKitchenStarted(true);
        assertTrue(order.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, order.getStatus());
        expectException(
            IllegalStateException.class,
            () -> order.setKitchenStarted(true));
        expectException(
            IllegalStateException.class,
            () -> order.setKitchenStarted(false));
        expectException(
            IllegalStateException.class,
            () -> order.setStatus(OrderStatus.CANCELLED));
        assertTrue(order.isKitchenStarted());
        assertEquals(OrderStatus.WAITING, order.getStatus());
        order.setStatus(OrderStatus.SERVED);
        assertEquals(OrderStatus.SERVED, order.getStatus());
        expectException(
            IllegalStateException.class,
            () -> order.setStatus(OrderStatus.WAITING));
    }


    /**
     * An order can be cancelled before cooking, and then nothing else.
     */
    public void testCancelBeforeCooking()
    {
        Order order = new Order(1, 1, items("Soup", 1), "", false);
        expectException(
            IllegalArgumentException.class,
            () -> order.setStatus(null));
        assertEquals(OrderStatus.WAITING, order.getStatus());
        order.setStatus(OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertFalse(order.isKitchenStarted());
        expectException(
            IllegalStateException.class,
            () -> order.setKitchenStarted(true));
        expectException(
            IllegalStateException.class,
            () -> order.setStatus(OrderStatus.SERVED));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }


    /**
     * toString shows the rush flag, notes, and cooking state.
     */
    public void testToString()
    {
        Order order = new Order(3, 4, items("Burger", 2), "no onions", true);
        assertEquals(
            "#3 table 4 RUSH {Burger=2} \"no onions\" WAITING",
            order.toString());
        order.setKitchenStarted(true);
        assertTrue(order.toString().endsWith("(cooking)"));
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
