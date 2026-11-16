
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a console-based interface for
 * the restaurant order tracking application.
 *
 * @author Prabhav Agarwal
 */
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
     * Starts the console application.
     *
     * @param controller the application controller
     */
    public void run(OrderTrackerController controller)
    {
        boolean running = true;

        System.out.println("==================================");
        System.out.println("     RESTAURANT ORDER TRACKER");
        System.out.println("==================================");

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
                    showMessage("Invalid choice. Please try again.");
                    break;
            }
        }
    }


    /**
     * Displays the available menu options.
     */
    public void displayMenu()
    {
        System.out.println();
        System.out.println("------------ MENU ------------");
        System.out.println("1. Add Order");
        System.out.println("2. Serve Next Order");
        System.out.println("3. Start Cooking");
        System.out.println("4. Cancel Last Order");
        System.out.println("5. Exit");
        System.out.println("------------------------------");
    }


    /**
     * Collects order details from the user
     * and sends them to the controller.
     *
     * @param controller the application controller
     */
    private void addOrder(OrderTrackerController controller)
    {
        System.out.println();
        System.out.println("---------- ADD ORDER ----------");

        System.out.print("Enter table number: ");
        String tableText = scanner.nextLine();

        for (String line : controller.getMenuLines())
        {
            System.out.println(line);
        }

        List<String> dishChoices = new ArrayList<>();
        List<String> quantityTexts = new ArrayList<>();

        while (true)
        {
            System.out.print("Dish number (0 to finish): ");
            String dishChoice = scanner.nextLine().trim();
            if (dishChoice.equals("0"))
            {
                break;
            }

            System.out.print("Quantity: ");
            String quantityText = scanner.nextLine().trim();

            dishChoices.add(dishChoice);
            quantityTexts.add(quantityText);
        }

        System.out.print("Notes (optional): ");
        String notes = scanner.nextLine();

        System.out.println("1. Normal");
        System.out.println("2. Rush");
        System.out.print("Select order mode: ");
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
            tableText, dishChoices, quantityTexts, notes, mode);
    }


    /**
     * Displays the next order and waiting count.
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
        System.out.println("========= ORDER STATUS =========");

        if (nextOrder == null || nextOrder.isBlank())
        {
            System.out.println("Next Order: None");
        }
        else
        {
            System.out.println("Next Order: " + nextOrder);
        }

        System.out.println("Waiting Orders: " + waitingCount);

        System.out.println("================================");
    }


    /**
     * Displays a message received from the controller.
     *
     * @param message success or error message
     */
    @Override
    public void showMessage(String message)
    {
        System.out.println();
        System.out.println("[ORDER TRACKER] " + message);
        System.out.println();
    }
}
