
/**
 * Counts successful order operations during one shift.
 */
public class OrderStatistics {
    private int totalAdded;
    private int rushAdded;
    private int served;
    private int cancelled;

    /**
     * Records a successful addition to the queue.
     * @param rush whether the added order is a rush order
     */
    public void incrementAdded(boolean rush) {
        totalAdded++;
        if (rush) {
            rushAdded++;
        }
    }

    /** Records a successful serve. */
    public void incrementServed() {
        served++;
    }

    /** Records a successful cancellation. */
    public void incrementCancelled() {
        cancelled++;
    }

    /** @return a printable summary of the current shift */
    public String getStats() {
        return "Shift summary"
            + "\nOrders added: " + totalAdded
            + "\nRush orders added: " + rushAdded
            + "\nOrders served: " + served
            + "\nOrders cancelled: " + cancelled
            + "\nOrders remaining: " + (totalAdded - served - cancelled);
    }
}
