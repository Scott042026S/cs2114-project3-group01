package src;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * // -------------------------------------------------------------------------
 * /** Connects the console view to the order queue. The view collects input and
 * calls the handle methods, the controller validates the input, calls the
 * model, and sends updated information back through the OrderTrackerDisplay
 * interface.
 * 
 * @author scottpost
 * @version Nov 15, 2026
 */
public class OrderTrackerController
{

    /** Fewest of one dish allowed on an order. */
    public static final int MIN_QUANTITY = 1;

    /** Most of one dish allowed on an order. */
    public static final int MAX_QUANTITY = 20;

    private final OrderQueue queue;
    private final Menu menu;
    private final WaitTimeEstimator estimator;
    private final OrderStatistics stats;
    private final OrderTrackerDisplay display;

    /**
     * Creates a controller for the given model objects and display.
     *
     * @param queue
     *            the order queue
     * @param menu
     *            the dishes and their prep times
     * @param estimator
     *            calculates wait times
     * @param stats
     *            end-of-shift statistics
     * @param display
     *            the display the controller sends updates to
     * @throws IllegalArgumentException
     *             if any argument is null
     */
    public OrderTrackerController(
        OrderQueue queue,
        Menu menu,
        WaitTimeEstimator estimator,
        OrderStatistics stats,
        OrderTrackerDisplay display)
    {
        if (queue == null || menu == null || estimator == null || stats == null
            || display == null)
        {
            throw new IllegalArgumentException(
                "Controller arguments cannot be null");
        }
        this.queue = queue;
        this.menu = menu;
        this.estimator = estimator;
        this.stats = stats;
        this.display = display;
    }


    /**
     * Gets the menu lines for the view to print, numbered from 1, like "1.
     * Burger (8 min)". The view uses this so it never touches the model
     * directly.
     *
     * @return the numbered menu lines
     */
    public List<String> getMenuLines()
    {
        List<String> lines = new ArrayList<String>();
        List<String> dishes = menu.getDishNames();
        for (int i = 0; i < dishes.size(); i++)
        {
            String dish = dishes.get(i);
            lines.add(
                (i + 1) + ". " + dish + " (" + menu.getPrepTime(dish)
                    + " min)");
        }
        return lines;
    }


    /**
     * Checks the typed table number.
     *
     * @param tableText
     *            the table number exactly as typed
     * @return null if valid, otherwise an error message
     */
    public String validateTable(String tableText)
    {
        if (tableText == null || tableText.trim().isEmpty())
        {
            return "Enter a table number";
        }
        String table = tableText.trim();
        if (table.length() > 3 || !table.matches("\\d+"))
        {
            // catches text, decimals, negatives, and very long input
            return "Enter a table number from 1 to " + Order.MAX_TABLES;
        }
        int number = Integer.parseInt(table);
        if (number < 1 || number > Order.MAX_TABLES)
        {
            return "Enter a table number from 1 to " + Order.MAX_TABLES;
        }
        return null;
    }


    /**
     * Checks the typed dish numbers and quantities.
     *
     * @param dishChoices
     *            dish numbers exactly as typed, one per dish
     * @param quantityTexts
     *            quantities exactly as typed, one per dish
     * @return null if valid, otherwise an error message
     */
    public
        String
        validateItems(List<String> dishChoices, List<String> quantityTexts)
    {
        if (dishChoices == null || quantityTexts == null
            || dishChoices.isEmpty())
        {
            return "Add at least one dish";
        }
        if (dishChoices.size() != quantityTexts.size())
        {
            return "Each dish needs a quantity";
        }
        int menuSize = menu.getDishNames().size();
        for (int i = 0; i < dishChoices.size(); i++)
        {
            Integer choice = parseWholeNumber(dishChoices.get(i));
            if (choice == null || choice < 1 || choice > menuSize)
            {
                return "Enter a dish number from 1 to " + menuSize;
            }
            Integer quantity = parseWholeNumber(quantityTexts.get(i));
            if (quantity == null || quantity < MIN_QUANTITY
                || quantity > MAX_QUANTITY)
            {
                return "Enter a quantity from " + MIN_QUANTITY + " to "
                    + MAX_QUANTITY;
            }
        }
        // the same dish picked more than once is combined, so check totals
        for (int total : buildItems(dishChoices, quantityTexts).values())
        {
            if (total > MAX_QUANTITY)
            {
                return "No more than " + MAX_QUANTITY + " of one dish";
            }
        }
        return null;
    }


    /**
     * Validates the input and adds the order to the queue.
     *
     * @param tableText
     *            the table number exactly as typed
     * @param dishChoices
     *            dish numbers exactly as typed, one per dish
     * @param quantityTexts
     *            quantities exactly as typed, one per dish
     * @param notes
     *            optional notes like "no onions"; may be blank
     * @param mode
     *            NORMAL or RUSH; null if the user picked neither
     */
    public void handleAddOrder(
        String tableText,
        List<String> dishChoices,
        List<String> quantityTexts,
        String notes,
        OrderMode mode)
    {
        if (mode == null)
        {
            display.showMessage("Please select a mode");
            return;
        }
        String error = validateTable(tableText);
        if (error == null)
        {
            error = validateItems(dishChoices, quantityTexts);
        }
        if (error != null)
        {
            display.showMessage(error);
            return;
        }
        boolean rush = (mode == OrderMode.RUSH);
        String cleanNotes = (notes == null) ? "" : notes.trim();
        try
        {
            Order order = queue.enqueue(
                Integer.parseInt(tableText.trim()),
                buildItems(dishChoices, quantityTexts),
                cleanNotes,
                rush);
            stats.incrementAdded(rush);
            display.showMessage("Added " + describe(order));
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            // invalid notes, or the table already has that order waiting
            display.showMessage(e.getMessage());
        }
        updateView();
    }


    /**
     * Marks the next order as being cooked.
     */
    public void handleStartCooking()
    {
        if (queue.isEmpty())
        {
            display.showMessage("No orders to start");
            return;
        }
        try
        {
            Order order = queue.startNext();
            display.showMessage("Started cooking " + describe(order));
        }
        catch (IllegalStateException e)
        {
            display.showMessage(e.getMessage());
        }
        updateView();
    }


    /**
     * Serves the order at the front of the line.
     */
    public void handleServeNext()
    {
        if (queue.isEmpty())
        {
            display.showMessage("No orders to serve");
            return;
        }
        try
        {
            Order order = queue.dequeue();
            stats.incrementServed();
            display.showMessage("Served " + describe(order));
        }
        catch (IllegalStateException e)
        {
            display.showMessage(e.getMessage());
        }
        updateView();
    }


    /**
     * Cancels the most recently added order if the kitchen has not started it.
     */
    public void handleCancelLast()
    {
        if (queue.isEmpty())
        {
            display.showMessage("No orders to cancel");
            return;
        }
        try
        {
            Order order = queue.cancelLast();
            stats.incrementCancelled();
            display.showMessage("Cancelled " + describe(order));
        }
        catch (IllegalStateException e)
        {
            display.showMessage(e.getMessage());
        }
        updateView();
    }


    /**
     * Shows the end-of-shift statistics.
     */
    public void handleEndShift()
    {
        display.showMessage(stats.getStats());
    }


    /**
     * Sends the next order (with its estimated wait) and the waiting count to
     * the display.
     */
    public void updateView()
    {
        Order next = queue.peek();
        if (next == null)
        {
            display.updateDisplay(null, 0);
            return;
        }
        String text = describe(next);
        if (next.isKitchenStarted())
        {
            text += " (cooking)";
        }
        int minutes = estimator.estimateWaitTime(next);
        if (minutes >= 0)
        {
            text += " - ready in about " + minutes + " min";
        }
        display.updateDisplay(text, queue.size());
    }


    /**
     * Turns typed dish numbers and quantities into dish names and totals. The
     * same dish picked more than once has its quantities added together.
     * Assumes the input already passed the number checks.
     *
     * @param dishChoices
     *            dish numbers as typed
     * @param quantityTexts
     *            quantities as typed
     * @return dish name to total quantity, in the order first picked
     */
    private
        Map<String, Integer>
        buildItems(List<String> dishChoices, List<String> quantityTexts)
    {
        List<String> dishes = menu.getDishNames();
        Map<String, Integer> items = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < dishChoices.size(); i++)
        {
            String dish = dishes.get(parseWholeNumber(dishChoices.get(i)) - 1);
            int quantity = parseWholeNumber(quantityTexts.get(i));
            Integer current = items.get(dish);
            items.put(dish, (current == null) ? quantity : current + quantity);
        }
        return items;
    }


    /**
     * Reads a short whole number typed by the user.
     *
     * @param text
     *            the typed text
     * @return the number, or null if the text is not a short whole number
     */
    private static Integer parseWholeNumber(String text)
    {
        if (text == null)
        {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > 3
            || !trimmed.matches("\\d+"))
        {
            return null;
        }
        return Integer.parseInt(trimmed);
    }


    /**
     * Builds a short display string for an order.
     *
     * @param order
     *            the order to describe
     * @return text like "order #3 (table 4): 2 Burger, 1 Fries [RUSH]"
     */
    private String describe(Order order)
    {
        StringBuilder text = new StringBuilder();
        text.append("order #").append(order.getOrderId()).append(" (table ")
            .append(order.getTableNumber()).append("): ");
        text.append(order.getDescription());
        if (!order.getNotes().isEmpty())
        {
            text.append(" (").append(order.getNotes()).append(")");
        }
        if (order.isRush())
        {
            text.append(" [RUSH]");
        }
        return text.toString();
    }
}
