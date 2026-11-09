import java.util.Scanner;

public class OrderTrackerView implements OrderTrackerDisplay
{
    //~ Fields

    private Scanner scanner;


    //~ Constructors

    /**
     * Creates the console view.
     */
    public OrderTrackerView()
    {
        scanner = new Scanner(System.in);
    }


    //~ Public Methods

    /**
     * Starts the console application menu.
     *
     * @param controller the application controller
     */
    public void run(OrderTrackerController controller)
    {
        boolean running = true;

        System.out.println("================================");
        System.out.println("   RESTAURANT ORDER TRACKER");
        System.out.println("================================");

        while (running)
        {
            displayMenu();

            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice)
            {
                case "1":
                    addOrder(controller);
                    break;

                case "2":
                    controller.handleServeNext();
                    break;

                case "3":
                    controller.handleStartCooking();
                    break;

                case "4":
                    controller.handleCancelLast();
                    break;

                case "5":
                    System.out.println("Exiting Order Tracker...");
                    running = false;
                    break;

                default:
                    showMessage("Invalid menu choice. Try again.");
                    break;
            }
        }
    }


    /**
     * Displays the main menu.
     */
    public void displayMenu()
    {
        System.out.println();
        System.out.println("---------- MENU ----------");
        System.out.println("1. Add Order");
        System.out.println("2. Serve Next");
        System.out.println("3. Start Cooking");
        System.out.println("4. Cancel Last");
        System.out.println("5. Exit");
        System.out.println("--------------------------");
    }


    /**
     * Collects order information and sends it
     * to the controller.
     *
     * @param controller the application controller
     */
    private void addOrder(OrderTrackerController controller)
    {
        System.out.println();
        System.out.println("---------- ADD ORDER ----------");

        System.out.print("Enter table number: ");
        String tableText = scanner.nextLine();

        System.out.print("Enter order description: ");
        String description = scanner.nextLine();

        System.out.println();
        System.out.println("Select order type:");
        System.out.println("1. Normal");
        System.out.println("2. Rush");

        System.out.print("Enter choice: ");
        String modeChoice = scanner.nextLine().trim();

        OrderMode mode = null;

        if (modeChoice.equals("1"))
        {
            mode = OrderMode.NORMAL;
        }
        else if (modeChoice.equals("2"))
        {
            mode = OrderMode.RUSH;
        }

        controller.handleAddOrder(
                tableText,
                description,
                mode
        );
    }


    /**
     * Updates the displayed order information.
     *
     * @param nextOrder description of the next order
     * @param waitingCount number of waiting orders
     */
    @Override
    public void updateDisplay(
            String nextOrder,
            int waitingCount)
    {
        System.out.println();
        System.out.println("======= ORDER STATUS =======");

        if (nextOrder == null || nextOrder.isBlank())
        {
            System.out.println("Next Order: None");
        }
        else
        {
            System.out.println("Next Order: " + nextOrder);
        }

        System.out.println(
                "Waiting Orders: " + waitingCount
        );

        System.out.println("============================");
    }


    /**
     * Displays a success or error message.
     *
     * @param message message to display
     */
    @Override
    public void showMessage(String message)
    {
        System.out.println();
        System.out.println("[ORDER TRACKER] " + message);
        System.out.println();
    }
}