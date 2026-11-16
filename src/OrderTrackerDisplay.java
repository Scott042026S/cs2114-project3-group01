
/**
 * Defines the communication between the controller
 * and the console-based order tracker view.
 *
 * @author Prabhav Agarwal
 */
public interface OrderTrackerDisplay
{
    /**
     * Updates the order information displayed
     * to the user.
     *
     * @param nextOrder description of the next order
     * @param waitingCount number of waiting orders
     */
    void updateDisplay(String nextOrder, int waitingCount);


    /**
     * Displays a success or error message.
     *
     * @param message message to display
     */
    void showMessage(String message);
}
