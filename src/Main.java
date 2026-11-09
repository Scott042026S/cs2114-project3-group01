
public class Main
{
    public static void main(String[] args)
    {
        OrderQueue queue = new OrderQueue();
        OrderTrackerView view = new OrderTrackerView();
        OrderTrackerController controller =
            new OrderTrackerController(queue, view);
        view.run(controller);
    }

}
