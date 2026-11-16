

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An immutable catalog of dishes and their prep times in minutes.
 */
public class Menu {
    public static final int DEFAULT_PREP_MINUTES = 10;

    private final List<String> dishNames;
    private final Map<String, Integer> prepMinutes;

    /** Creates the restaurant's default menu in display order. */
    public Menu() {
        this(defaultDishes());
    }

    /**
     * Copies a custom menu in the map's iteration order.
     * @param dishes dish names and positive prep times in minutes
     */
    public Menu(Map<String, Integer> dishes) {
        if (dishes == null) {
            throw new IllegalArgumentException("Menu is required");
        }
        List<String> names = new ArrayList<>();
        Map<String, Integer> minutes = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : dishes.entrySet()) {
            String name = normalize(entry.getKey());
            Integer time = entry.getValue();
            if (name.isEmpty() || time == null || time <= 0
                || minutes.containsKey(name)) {
                throw new IllegalArgumentException("Invalid menu entry");
            }
            names.add(entry.getKey().trim());
            minutes.put(name, time);
        }
        dishNames = Collections.unmodifiableList(names);
        prepMinutes = Collections.unmodifiableMap(minutes);
    }

    /** @return the read-only dish names in display order */
    public List<String> getDishNames() {
        return dishNames;
    }

    /**
     * @param dish dish name
     * @return the prep time for one item, or 10 minutes for an unknown dish
     */
    public int getPrepTime(String dish) {
        return prepMinutes.getOrDefault(normalize(dish), DEFAULT_PREP_MINUTES);
    }

    // Lets the estimator distinguish a missing dish from a 10-minute dish.
    Integer findPrepTime(String dish) {
        return prepMinutes.get(normalize(dish));
    }

    private static String normalize(String dish) {
        return dish == null ? "" : dish.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, Integer> defaultDishes() {
        Map<String, Integer> dishes = new LinkedHashMap<>();
        dishes.put("Burger", 8);
        dishes.put("Fries", 4);
        dishes.put("Chicken Sandwich", 10);
        dishes.put("Salad", 5);
        return dishes;
    }
}
