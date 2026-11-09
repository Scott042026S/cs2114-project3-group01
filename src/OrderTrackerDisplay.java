public interface OrderTrackerDisplay
{
    /**
     * Updates the information displayed to the user.
     *
     * @param nextOrder description of the next order
     * @param waitingCount number of orders waiting
     */
    void updateDisplay(String nextOrder, int waitingCount);

    /**
     * Displays a success or error message.
     *
     * @param message message to display
     */
    void showMessage(String message);
}