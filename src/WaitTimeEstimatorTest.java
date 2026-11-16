import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import student.TestCase;

/**
 * Tests WaitTimeEstimator using the default menu (Burger 8, Fries 4,
 * Chicken Sandwich 10, Salad 5).
 *
 * @author Scott Post
 * @version Nov 16, 2026
 */
public class WaitTimeEstimatorTest extends TestCase
{
    private OrderQueue queue;
    private WaitTimeEstimator estimator;

    /**
     * Creates an empty queue and an estimator before each test.
     */
    public void setUp()
    {
        queue = new OrderQueue();
        estimator = new WaitTimeEstimator(queue, new Menu());
    }


    /**
     * Builds a dish map from name/quantity pairs.
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
     * Prep time is quantity times menu time, and notes don't change it.
     */
    public void testPrepTimeUsesDishesNotNotes()
    {
        Order order = queue.enqueue(1, items("Burger", 2, "Fries", 1),
            "20 fries please", false);
        assertEquals(20, estimator.estimatePrepTime(order));
    }


    /**
     * A dish that isn't on the menu uses the default time.
     */
    public void testUnknownDishUsesDefault()
    {
        Order order = queue.enqueue(1, items("Soup", 2), "", false);
        assertEquals(2 * WaitTimeEstimator.DEFAULT_PREP_MINUTES,
            estimator.estimatePrepTime(order));
        assertEquals(5, estimator.getPrepTime("salad"));
    }


    /**
     * Wait time adds up every order ahead in serve order, rush first.
     */
    public void testWaitTimeFollowsServeOrder()
    {
        Order normal = queue.enqueue(1, items("Salad", 1), "", false);
        Order rush = queue.enqueue(2, items("Burger", 1), "", true);
        assertEquals(8, estimator.estimateWaitTime(rush));
        assertEquals(13, estimator.estimateWaitTime(normal));
    }


    /**
     * An order that isn't in the list gives -1, and bad input is rejected.
     */
    public void testAbsentOrderAndBadInput()
    {
        Order outside = new Order(99, 1, items("Fries", 1), "", false);
        assertEquals(-1, estimator.estimateWaitTime(outside));
        expectException(IllegalArgumentException.class,
            () -> estimator.estimatePrepTime(null));
        expectException(IllegalArgumentException.class,
            () -> new WaitTimeEstimator(null, new Menu()));
    }


    /**
     * The list-only estimator needs a list supplied.
     */
    public void testListOnlyEstimator()
    {
        WaitTimeEstimator listOnly =
            new WaitTimeEstimator(items("Taco", 3));
        Order order = new Order(1, 1, items("Taco", 2), "", false);
        assertEquals(6,
            listOnly.estimateWaitTime(order, Arrays.asList(order)));
        expectException(IllegalStateException.class,
            () -> listOnly.estimateWaitTime(order));
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