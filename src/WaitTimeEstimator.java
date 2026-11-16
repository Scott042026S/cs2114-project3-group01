import java.util.List;
import java.util.Map;

/**
 * Estimates ready times from each order's dishes and the queue's serve order.
 */
public class WaitTimeEstimator
{
    public static final int DEFAULT_PREP_MINUTES = Menu.DEFAULT_PREP_MINUTES;

    private final OrderQueue queue;
    private final Menu menu;

    /**
     * Creates an estimator for the live queue.
     * 
     * @param queue
     *            the queue that supplies the current serving order
     * @param menu
     *            the menu containing dish prep times
     */
    public WaitTimeEstimator(OrderQueue queue, Menu menu)
    {
        if (queue == null || menu == null)
        {
            throw new IllegalArgumentException("Queue and menu are required");
        }
        this.queue = queue;
        this.menu = menu;
    }


    /**
     * Creates an estimator for use with an explicit serving-order list.
     * 
     * @param prepMinutes
     *            dish names and positive prep times in minutes
     */
    public WaitTimeEstimator(Map<String, Integer> prepMinutes)
    {
        queue = null;
        menu = new Menu(prepMinutes);
    }


    /**
     * @param order
     *            the order to estimate
     * @return minutes until ready, or -1 if the order is absent
     */
    public int estimateWaitTime(Order order)
    {
        if (queue == null)
        {
            throw new IllegalStateException("Supply a list in serve order");
        }
        return estimateWaitTime(order, queue.getOrdersInServeOrder());
    }


    /**
     * @param dish
     *            a dish name
     * @return the minutes per item, or 10 if the dish is unknown
     */
    public int getPrepTime(String dish)
    {
        return menu.getPrepTime(dish);
    }


    /**
     * Adds up prep time for every dish on the order: quantity times the dish's
     * prep time from the menu. Notes are ignored.
     * 
     * @param order
     *            the order to estimate
     * @return the prep time in minutes, capped at Integer.MAX_VALUE
     */
    public int estimatePrepTime(Order order)
    {
        if (order == null)
        {
            throw new IllegalArgumentException("Order is required");
        }
        long total = 0;
        for (Map.Entry<String, Integer> item : order.getItems().entrySet())
        {
            total += (long)item.getValue() * menu.getPrepTime(item.getKey());
            if (total >= Integer.MAX_VALUE)
            {
                return Integer.MAX_VALUE;
            }
        }
        return (int)total;
    }


    /**
     * Adds prep time for every order ahead of the target and the target itself.
     * 
     * @param order
     *            the target order
     * @param ordersInServeOrder
     *            a snapshot from OrderQueue.getOrdersInServeOrder()
     * @return minutes until ready, or -1 if the target is absent
     */
    public int estimateWaitTime(Order order, List<Order> ordersInServeOrder)
    {
        if (order == null || ordersInServeOrder == null)
        {
            throw new IllegalArgumentException(
                "Order and service list are required");
        }
        long total = 0;
        for (Order waiting : ordersInServeOrder)
        {
            total += estimatePrepTime(waiting);
            if (waiting == order)
            {
                return (int)Math.min(total, Integer.MAX_VALUE);
            }
        }
        return -1;
    }
}
