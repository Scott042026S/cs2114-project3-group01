
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import student.TestCase;

/** Tests menu numbering, lookup, and protection of menu data. */
public class MenuTest extends TestCase
{
    public void testDefaultMenuOrderAndPrepTimes()
    {
        Menu menu = new Menu();

        assertEquals(Arrays.asList("Burger", "Fries", "Chicken Sandwich",
            "Salad"), menu.getDishNames());
        assertEquals(8, menu.getPrepTime("Burger"));
        assertEquals(4, menu.getPrepTime("Fries"));
        assertEquals(10, menu.getPrepTime("Chicken Sandwich"));
        assertEquals(5, menu.getPrepTime("Salad"));
    }

    public void testLookupIgnoresCaseAndSurroundingSpaces()
    {
        Menu menu = new Menu();

        assertEquals(8, menu.getPrepTime("  bUrGeR  "));
        assertEquals(10, menu.getPrepTime("Soup"));
        assertEquals(10, menu.getPrepTime(" "));
        assertEquals(10, menu.getPrepTime(null));
    }

    public void testCustomMenuCopiesInputAndProtectsDishList()
    {
        Map<String, Integer> dishes = new LinkedHashMap<>();
        dishes.put("Soup", 6);
        dishes.put("Pasta", 12);
        Menu menu = new Menu(dishes);

        dishes.put("Soup", 99);
        dishes.put("Salad", 5);

        assertEquals(Arrays.asList("Soup", "Pasta"), menu.getDishNames());
        assertEquals(6, menu.getPrepTime("Soup"));

        try
        {
            menu.getDishNames().clear();
            fail("The dish list should be read-only.");
        }
        catch (UnsupportedOperationException expected)
        {
            assertEquals(2, menu.getDishNames().size());
        }
    }

    public void testRejectsNonpositivePrepTime()
    {
        try
        {
            new Menu(Map.of("Soup", 0));
            fail("A prep time of zero should be rejected.");
        }
        catch (IllegalArgumentException expected)
        {
            assertNotNull(expected.getMessage());
        }
    }
}
