import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests OrderTrackerController without the real view. A fake display records
 * every message and update so the tests can check them.
 *
 * @author Scott Post
 * @version Nov 15, 2026
 */
public class OrderTrackerControllerTest
    extends student.TestCase
{

    /**
     * Stands in for the console view and remembers what it was sent.
     */
    private static class FakeDisplay
        implements OrderTrackerDisplay
    {
        private List<String> messages = new ArrayList<String>();
        private String lastNextOrder;
        private int lastWaitingCount = -1;

        @Override
        public void updateDisplay(String nextOrder, int waitingCount)
        {
            lastNextOrder = nextOrder;
            lastWaitingCount = waitingCount;
        }


        @Override
        public void showMessage(String message)
        {
            messages.add(message);
        }


        public String lastMessage()
        {
            return messages.isEmpty()
                ? null
                : messages.get(messages.size() - 1);
        }
    }

    private OrderQueue queue;
    private Menu menu;
    private OrderStatistics stats;
    private FakeDisplay display;
    private OrderTrackerController controller;

    /**
     * Creates a fresh controller before each test.
     */
    public void setUp()
    {
        queue = new OrderQueue();
        menu = new Menu();
        stats = new OrderStatistics();
        display = new FakeDisplay();
        controller = new OrderTrackerController(
            queue,
            menu,
            new WaitTimeEstimator(queue, menu),
            stats,
            display);
    }


    /**
     * Adds a one-dish order through the controller.
     */
    private void add(String table, String dish, String quantity, OrderMode mode)
    {
        controller.handleAddOrder(
            table,
            Arrays.asList(dish),
            Arrays.asList(quantity),
            "",
            mode);
    }

    // ----- constructor


    /**
     * A null argument is rejected.
     */
    public void testConstructorNull()
    {
        Exception thrown = null;
        try
        {
            new OrderTrackerController(
                null,
                menu,
                new WaitTimeEstimator(queue, menu),
                stats,
                display);
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
    }

    // ----- handleAddOrder


    /**
     * A valid order is added and shown.
     */
    public void testAddOrderNormal()
    {
        add("4", "1", "2", OrderMode.NORMAL);
        assertEquals(1, queue.size());
        assertTrue(display.lastMessage().startsWith("Added"));
        assertEquals(1, display.lastWaitingCount);
        assertNotNull(display.lastNextOrder);
    }


    /**
     * Picking the same dish twice combines the quantities.
     */
    public void testAddOrderCombinesSameDish()
    {
        controller.handleAddOrder(
            "4",
            Arrays.asList("1", "1"),
            Arrays.asList("2", "3"),
            "",
            OrderMode.NORMAL);
        String dish = menu.getDishNames().get(0);
        assertEquals(5, (int)queue.peek().getItems().get(dish));
    }


    /**
     * No mode selected: nothing is added.
     */
    public void testAddOrderNoMode()
    {
        add("4", "1", "2", null);
        assertTrue(queue.isEmpty());
        assertEquals("Please select a mode", display.lastMessage());
    }


    /**
     * Bad table numbers: nothing is added.
     */
    public void testAddOrderBadTable()
    {
        String[] bad = { "", "   ", "abc", "4.5", "-3", "0", "51", "99999" };
        for (String table : bad)
        {
            add(table, "1", "2", OrderMode.NORMAL);
        }
        assertTrue(queue.isEmpty());
        assertEquals(bad.length, display.messages.size());
    }


    /**
     * Bad dish numbers: nothing is added.
     */
    public void testAddOrderBadDish()
    {
        String tooHigh = String.valueOf(menu.getDishNames().size() + 1);
        add("4", "0", "1", OrderMode.NORMAL);
        add("4", tooHigh, "1", OrderMode.NORMAL);
        add("4", "abc", "1", OrderMode.NORMAL);
        assertTrue(queue.isEmpty());
    }


    /**
     * Bad quantities: nothing is added.
     */
    public void testAddOrderBadQuantity()
    {
        add("4", "1", "0", OrderMode.NORMAL);
        add("4", "1", "21", OrderMode.NORMAL);
        add("4", "1", "two", OrderMode.NORMAL);
        controller.handleAddOrder(
            "4",
            Arrays.asList("1", "1"),
            Arrays.asList("15", "15"),
            "",
            OrderMode.NORMAL);
        assertTrue(queue.isEmpty());
    }


    /**
     * No dishes picked: nothing is added.
     */
    public void testAddOrderNoDishes()
    {
        controller.handleAddOrder(
            "4",
            new ArrayList<String>(),
            new ArrayList<String>(),
            "",
            OrderMode.NORMAL);
        assertTrue(queue.isEmpty());
        assertEquals("Add at least one dish", display.lastMessage());
    }


    /**
     * The same order twice for one table shows the model's message instead of
     * crashing.
     */
    public void testAddOrderDuplicate()
    {
        add("4", "1", "2", OrderMode.NORMAL);
        add("4", "1", "2", OrderMode.NORMAL);
        assertEquals(1, queue.size());
    }

    // ----- handleServeNext


    /**
     * Serving removes the order and counts it.
     */
    public void testServeNextNormal()
    {
        add("4", "1", "2", OrderMode.NORMAL);
        controller.handleServeNext();
        assertTrue(queue.isEmpty());
        assertTrue(display.lastMessage().startsWith("Served"));
    }


    /**
     * Serving an empty queue shows a message.
     */
    public void testServeNextEmpty()
    {
        controller.handleServeNext();
        assertEquals("No orders to serve", display.lastMessage());
    }

    // ----- handleStartCooking


    /**
     * Starting marks the next order as cooking.
     */
    public void testStartCookingNormal()
    {
        add("4", "1", "2", OrderMode.NORMAL);
        controller.handleStartCooking();
        assertTrue(queue.peek().isKitchenStarted());
    }


    /**
     * Starting an empty queue shows a message.
     */
    public void testStartCookingEmpty()
    {
        controller.handleStartCooking();
        assertEquals("No orders to start", display.lastMessage());
    }


    /**
     * Starting an order that is already cooking does not crash.
     */
    public void testStartCookingTwice()
    {
        add("4", "1", "2", OrderMode.NORMAL);
        controller.handleStartCooking();
        controller.handleStartCooking();
        assertTrue(queue.peek().isKitchenStarted());
        assertEquals(1, queue.size());
    }

    // ----- handleCancelLast


    /**
     * Cancelling removes the newest order.
     */
    public void testCancelLastNormal()
    {
        add("4", "1", "2", OrderMode.NORMAL);
        add("7", "1", "1", OrderMode.NORMAL);
        controller.handleCancelLast();
        assertEquals(1, queue.size());
        assertEquals(4, queue.peek().getTableNumber());
    }


    /**
     * Cancelling an empty queue shows a message.
     */
    public void testCancelLastEmpty()
    {
        controller.handleCancelLast();
        assertEquals("No orders to cancel", display.lastMessage());
    }


    /**
     * An order the kitchen started cannot be cancelled.
     */
    public void testCancelLastStarted()
    {
        add("4", "1", "2", OrderMode.NORMAL);
        controller.handleStartCooking();
        controller.handleCancelLast();
        assertEquals(1, queue.size());
    }

    // ----- updateView


    /**
     * An empty queue sends null and 0 to the display.
     */
    public void testUpdateViewEmpty()
    {
        controller.updateView();
        assertNull(display.lastNextOrder);
        assertEquals(0, display.lastWaitingCount);
    }


    /**
     * The next order includes its wait estimate and rush label.
     */
    public void testUpdateViewRushWithEstimate()
    {
        add("4", "1", "2", OrderMode.RUSH);
        assertTrue(display.lastNextOrder.contains("[RUSH]"));
        assertTrue(display.lastNextOrder.contains("min"));
    }

    // ----- getMenuLines


    /**
     * Menu lines are numbered from 1, one per dish.
     */
    public void testGetMenuLines()
    {
        List<String> lines = controller.getMenuLines();
        assertEquals(menu.getDishNames().size(), lines.size());
        assertTrue(lines.get(0).startsWith("1. "));
    }
}
