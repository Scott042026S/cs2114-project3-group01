
public class Main
{
    public static void main(String[] args)
    {
        OrderQueue queue = new OrderQueue();
        Menu menu = new Menu();
        WaitTimeEstimator estimator = new WaitTimeEstimator(queue, menu);
        OrderStatistics stats = new OrderStatistics();
        OrderTrackerView view = new OrderTrackerView();
        OrderTrackerController controller =
            new OrderTrackerController(queue, menu, estimator, stats, view);
        view.run(controller);
    }

}
