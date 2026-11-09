/**
 * // -------------------------------------------------------------------------
 * /** Connects the console view to the order queue. The view collects input and
 * calls the handle methods, the controller validates the input, calls the
 * model, and sends updated information back through the OrderTrackerDisplay
 * interface.
 * 
 * @author scottpost
 * @version Nov 8, 2026
 */
public class OrderTrackerController
{

    private final OrderQueue queue;
    private final OrderTrackerDisplay display;

    /**
     * Creates a controller for the given model and display.
     *
     * @param queue
     *            the order queue (model)
     * @param display
     *            the display the controller sends updates to
     * @throws IllegalArgumentException
     *             if either argument is null
     */
    public OrderTrackerController(OrderQueue queue, OrderTrackerDisplay display)
    {
        if (queue == null || display == null)
        {
            throw new IllegalArgumentException(
                "Queue and display cannot be null");
        }
        this.queue = queue;
        this.display = display;
    }


    /**
     * Checks the typed table number and description.
     *
     * @param tableText
     *            the table number exactly as typed
     * @param description
     *            the order description exactly as typed
     * @return null if both are valid, otherwise an error message
     */
    public String validateInput(String tableText, String description)
    {
        if (tableText == null || tableText.trim().isEmpty())
        {
            return "Enter a table number";
        }
        String table = tableText.trim();
        if (table.length() > 3 || !table.matches("\\d+"))
        {
            // catches text, decimals, negatives, and very long input
            return Order.INVALID_TABLE_MESSAGE;
        }
        try
        {
            // Order owns the rules for table range and description
            Order.validate(Integer.parseInt(table), description);
        }
        catch (IllegalArgumentException e)
        {
            return e.getMessage();
        }
        return null;
    }


    /**
     * Validates the input and adds the order to the queue.
     *
     * @param tableText
     *            the table number exactly as typed
     * @param description
     *            the order description exactly as typed
     * @param mode
     *            NORMAL or RUSH; null if the user picked neither
     */
    public
        void
        handleAddOrder(String tableText, String description, OrderMode mode)
    {
        if (mode == null)
        {
            display.showMessage("Please select a mode");
            return;
        }
        String error = validateInput(tableText, description);
        if (error != null)
        {
            display.showMessage(error);
            return;
        }
        boolean rush = (mode == OrderMode.RUSH);
        try
        {
            Order order = queue.enqueue(
                Integer.parseInt(tableText.trim()),
                description.trim(),
                rush);
            display.showMessage("Added " + describe(order));
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            // invalid input, or the table already has that order waiting
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
            display.showMessage("Cancelled " + describe(order));
        }
        catch (IllegalStateException e)
        {
            display.showMessage(e.getMessage());
        }
        updateView();
    }


    /**
     * Sends the next order and the waiting count to the display.
     */
    public void updateView()
    {
        Order next = queue.peek();
        if (next == null)
        {
            display.updateDisplay(null, 0);
        }
        else
        {
            String text = describe(next);
            if (next.isKitchenStarted())
            {
                text += " (cooking)";
            }
            display.updateDisplay(text, queue.size());
        }
    }


    /**
     * Builds a short display string for an order.
     *
     * @param order
     *            the order to describe
     * @return text like "order #3 (table 4): 2 burgers [RUSH]"
     */
    private String describe(Order order)
    {
        String text = "order #" + order.getOrderId() + " (table "
            + order.getTableNumber() + "): " + order.getDescription();
        if (order.isRush())
        {
            text += " [RUSH]";
        }
        return text;
    }
}
