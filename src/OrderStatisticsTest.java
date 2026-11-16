

import student.TestCase;

/** Tests the shift summary and the counts used to build it. */
public class OrderStatisticsTest extends TestCase
{
    private OrderStatistics statistics;

    @Override
    public void setUp()
    {
        statistics = new OrderStatistics();
    }

    public void testNewShiftStartsWithZeroCounts()
    {
        assertSummary(0, 0, 0, 0, 0);
    }

    public void testRushOrdersAlsoCountTowardTotalAdded()
    {
        statistics.incrementAdded(false);
        statistics.incrementAdded(true);
        statistics.incrementAdded(true);

        assertSummary(3, 2, 0, 0, 3);
    }

    public void testServingAndCancellingReduceRemainingCount()
    {
        statistics.incrementAdded(false);
        statistics.incrementAdded(true);
        statistics.incrementAdded(false);
        statistics.incrementServed();
        statistics.incrementCancelled();

        assertSummary(3, 1, 1, 1, 1);

        statistics.incrementServed();
        assertSummary(3, 1, 2, 1, 0);
    }

    private void assertSummary(int added, int rush, int served,
        int cancelled, int remaining)
    {
        String expected = "Shift summary"
            + "\nOrders added: " + added
            + "\nRush orders added: " + rush
            + "\nOrders served: " + served
            + "\nOrders cancelled: " + cancelled
            + "\nOrders remaining: " + remaining;

        assertEquals(expected, statistics.getStats());
    }
}
