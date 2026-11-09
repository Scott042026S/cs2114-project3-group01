/**
 * The possible states of an order.
 *
 * @author Archer Kim
 * @version 11/08/2026
 */
public enum OrderStatus {

    /** Still in the queue. */
    WAITING,

    /** Given to the table. */
    SERVED,

    /** Cancelled before the kitchen started it. */
    CANCELLED
}
