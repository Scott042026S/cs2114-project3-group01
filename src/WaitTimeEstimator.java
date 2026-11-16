package src;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estimates ready times using order descriptions and the queue's service order.
 */
public class WaitTimeEstimator {
    public static final int DEFAULT_PREP_MINUTES = Menu.DEFAULT_PREP_MINUTES;

    private static final Pattern QUANTITY = Pattern.compile("^(\\d+)\\s+(.+)$");

    private final OrderQueue queue;
    private final Menu menu;

    /**
     * Creates an estimator for the live queue.
     * @param queue the queue that supplies the current serving order
     * @param menu the menu containing dish prep times
     */
    public WaitTimeEstimator(OrderQueue queue, Menu menu) {
        if (queue == null || menu == null) {
            throw new IllegalArgumentException("Queue and menu are required");
        }
        this.queue = queue;
        this.menu = menu;
    }

    /**
     * Creates an estimator for use with an explicit serving-order list.
     * @param prepMinutes dish names and positive prep times in minutes
     */
    public WaitTimeEstimator(Map<String, Integer> prepMinutes) {
        queue = null;
        menu = new Menu(prepMinutes);
    }

    /**
     * @param order the order to estimate
     * @return minutes until ready, or -1 if the order is absent
     */
    public int estimateWaitTime(Order order) {
        if (queue == null) {
            throw new IllegalStateException("Supply a list in serve order");
        }
        return estimateWaitTime(order, queue.getOrdersInServeOrder());
    }

    /**
     * @param dish a dish name
     * @return the minutes per item, or 10 if the dish is unknown
     */
    public int getPrepTime(String dish) {
        return menu.getPrepTime(dish);
    }

    /**
     * Reads comma-separated items such as "2 burgers, fries".
     * @param order the order to estimate
     * @return the prep time in minutes, capped at Integer.MAX_VALUE
     */
    public int estimatePrepTime(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }

        long total = 0;
        for (String entry : order.getDescription().split(",")) {
            String dish = normalize(entry);
            if (dish.isEmpty()) {
                continue;
            }

            int quantity = 1;
            Matcher match = QUANTITY.matcher(dish);
            if (match.matches()) {
                quantity = parseQuantity(match.group(1));
                dish = match.group(2);
            }

            // Prefer an exact menu entry before trying a singular form.
            Integer minutes = menu.findPrepTime(dish);
            if (minutes == null && dish.endsWith("s")) {
                minutes = menu.findPrepTime(dish.substring(0, dish.length() - 1));
            }
            int perItem = minutes == null ? DEFAULT_PREP_MINUTES : minutes;
            total += (long)quantity * perItem;
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int)total;
    }

    /**
     * Adds prep time for every order ahead of the target and the target itself.
     * @param order the target order
     * @param ordersInServeOrder a snapshot from OrderQueue.getOrdersInServeOrder()
     * @return minutes until ready, or -1 if the target is absent
     */
    public int estimateWaitTime(Order order, List<Order> ordersInServeOrder) {
        if (order == null || ordersInServeOrder == null) {
            throw new IllegalArgumentException("Order and service list are required");
        }
        long total = 0;
        for (Order waiting : ordersInServeOrder) {
            total += estimatePrepTime(waiting);
            if (waiting == order) {
                return (int)Math.min(total, Integer.MAX_VALUE);
            }
        }
        return -1;
    }

    private static String normalize(String dish) {
        return dish == null ? "" : dish.trim().toLowerCase(Locale.ROOT);
    }

    private static int parseQuantity(String text) {
        try {
            return Math.max(1, Integer.parseInt(text));
        }
        catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
