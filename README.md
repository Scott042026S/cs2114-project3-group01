# cs2114-project3-group01

A console app for a restaurant kitchen. Servers add orders by table number, picking dishes and quantities from a menu, and the kitchen always works on the oldest order first. Rush orders are served ahead of normal orders, but stay first-in, first-out among themselves. The most recent order can be cancelled as long as the kitchen hasn't started cooking it. Each order shows an estimated wait, and the end-of-shift option shows how many orders were added, served, and cancelled.

## How to Run

**The app:** in Eclipse, open `src/Main.java` and choose **Run As → Java Application**. The menu appears in the Console panel; type a number and press Enter.

1. **Add Order:** enter a table number (1–50), then dish numbers from the menu with a quantity for each (1–20). Press Enter on a blank line to finish, add optional notes (like "no onions"), and pick 1 for Normal or 2 for Rush.
2. **Serve Next Order**
3. **Start Cooking:** marks the next order as cooking, so it can no longer be cancelled.
4. **Cancel Last Order**
5. **End Shift (show stats)**
6. **Exit**

**The tests:** right-click the `src` folder and choose **Run As → JUnit Test** to run all four test classes (`OrderTest`, `OrderQueueTest`, `WaitTimeEstimatorTest`, `OrderTrackerControllerTest`). None of them open the view: the model tests create objects directly, and the controller test uses a fake display that records messages. The tests need `lib/student.jar`, which is already on the project's build path.

---

# Prototype 2 (Deliverable 4)

## What we added

Our change feature was **estimated wait times**, built with a menu so the app knows exactly what is in each order:

- **`Menu`** (new, model): the dishes and their prep times (Burger 8 min, Fries 4, Chicken Sandwich 10, Salad 5), in a fixed order so the view can number them.
- **`Order`**: now stores the dishes as a `Map<String, Integer>` (dish → quantity). The old description became optional notes.
- **`OrderQueue`**: `enqueue(int, Map<String, Integer>, String, boolean)` takes the dishes, and the duplicate-order check compares dishes and notes instead of a description.
- **`WaitTimeEstimator`**: an order's prep time is quantity × menu prep time for each dish; its wait is its own prep time plus the prep time of every order ahead of it in `getOrdersInServeOrder()`.
- **`OrderStatistics`**: counts added, rush, served, and cancelled orders for the end-of-shift summary.
- **Controller**: `handleAddOrder` takes the typed dish numbers and quantities, validates them, combines a dish picked twice, and builds the map. `updateView()` adds "ready in about X min." New `handleEndShift()` and `getMenuLines()`.
- **View**: prints the dishes, collects dishes until a blank line, then notes; new End Shift option.

## Reflection

### Our prediction vs. what actually happened

In Deliverable 3 we predicted which classes the wait-time feature would touch. Here is how that held up:

| Class | Predicted | What actually happened | Why |
|---|---|---|---|
| `WaitTimeEstimator` | New: `estimateWaitTime(Order)`, `getPrepTime(String)` | **As predicted**, plus `estimatePrepTime(Order)` and a version of `estimateWaitTime` that takes a list, so it can be tested without a queue. | Testing it on its own was easier with a plain list. |
| `OrderTrackerController` | Constructor and `updateView()` | **More than predicted.** Constructor and `updateView()` changed, but so did `handleAddOrder` (new parameters and validation), and we added `getMenuLines()`, `handleEndShift()`, and stats calls in every handler. | Choosing a menu meant the controller had to validate dish numbers and quantities instead of free text. |
| `OrderTrackerDisplay` / `OrderTrackerView.updateDisplay` | New parameter for the estimate | **Wrong.** `OrderTrackerDisplay` didn't change at all. The estimate is part of the next-order text the controller already sends. The view changed in different places: the add-order prompts and the menu options. | Putting the estimate in the text kept the interface stable. |
| `Main` | Create the estimator | **As predicted**, plus `Menu` and `OrderStatistics`. | |
| `Order`, `OrderQueue` | **Should not change** | **Wrong. Both changed.** `Order` now stores a dish map, and `enqueue`, `addOrder`, and `hasWaitingOrder` take the dishes. | Our prediction assumed free-text descriptions. Once we chose a menu, orders needed structured dish data, and that lives in the model. |
| `OrderStatus`, `OrderMode` | Should not change | **As predicted.** | |
| `Menu` | Not predicted | **New class.** | Keeping prep times in their own class let the estimator and the controller share one list of dishes. |

The main lesson: the prediction was right about the controller and estimator, but it missed that a decision about *input* (menu vs. free text) would reach all the way into the model's data. The model/view boundary still held, though: the view never touches `Menu` directly, since it prints the menu through `controller.getMenuLines()`.

### What changed from the spec and Prototype 1

| Area | Before | Now | Why |
|---|---|---|---|
| Order contents | A single description string, validated by `Order.validate(int, String)`. | A dish map plus optional notes, validated by `Order.validate(int, Map, String)`. | Needed for wait times; see above. |
| Wait-time input | The spec had `WaitTimeEstimator` parse dish names from the description. | It reads `getItems()` and never parses text. | Parsing free text ("two burgers," typos) was error-prone. An early version still read `getDescription()` and gave 0 minutes for orders without notes; switching to `getItems()` fixed it. |
| Validation | The controller checked the table number, then called `Order.validate`. | The controller checks the table, each dish number (on the menu), each quantity (1–20, including combined totals), and the mode. `Order.validate` checks the dish map and notes. | Typed text can only be checked in the controller; the rules about what a valid order is stay in the model. |
| Duplicate orders | Same table with the same description waiting was rejected. | Same table with the same dishes and notes waiting is rejected. | Matches the new order format. |
| Controller constructor | `OrderQueue`, `OrderTrackerDisplay` | Adds `Menu`, `WaitTimeEstimator`, and `OrderStatistics`. | Planned for Prototype 2 in our D3 notes. |
| View menu | 5 options | 6 options (End Shift added). The menu now prints before waiting for input. | Stats feature, and a bug where the options didn't appear until you typed something. |

### Did the model stand alone?

Yes. `Order`, `OrderQueue`, `Menu`, `WaitTimeEstimator`, and `OrderStatistics` don't reference the view or the controller, and their tests run without the view. Nothing had to move across the model/view line to add the feature.

### Tests

We now have 42 tests across four classes:

- **`OrderTest` (9):** construction, blank notes allowed, dish map copied and unmodifiable, table and quantity boundaries, invalid dishes and notes, lifecycle (start, serve, cancel).
- **`OrderQueueTest` (8):** empty queue, rush priority and FIFO within each group, the cooking order staying first, cancelling the newest across both deques, refusing to cancel a cooking order, rejected adds leaving the queue unchanged.
- **`WaitTimeEstimatorTest` (5):** prep time from dishes (not notes), unknown dishes using the default, waits following serve order, absent orders and bad input.
- **`OrderTrackerControllerTest` (20):** every `handle...` method with a fake display, including bad tables (blank, text, decimals, negative, 0, 51), bad dish numbers, bad quantities (0, 21, text, combined over 20), no dishes, no mode, duplicates, and serving, starting, or cancelling an empty queue.

### What doesn't work yet

- **The wait estimate assumes the kitchen cooks one order at a time,** and an order that is already cooking counts at its full prep time, not its remaining time.
- **The display shows only the next order,** not the full line with each order's wait.
- **A table can't repeat an identical order** (same dishes and notes) while the first is still waiting.
- **The menu is fixed in code;** there's no way to change dishes while the app runs.

## Next Steps (for the final)

### Stretch goals

| Stretch goal | Status |
|---|---|
| Rush limits (rush orders served in the order they were rushed) | **Done** in Prototype 1. |
| Estimated wait times | **Done** in Prototype 2. |
| End-of-shift stats | **Done** in Prototype 2. |
| Small orders first (a short order can be served ahead of a much larger one, but a large order can be skipped at most twice so it never waits forever) | **Committed** for the final. Now possible because `WaitTimeEstimator.estimatePrepTime(Order)` can tell large orders from small ones. |

### Remaining work, in order

1. **Small orders first:** change `OrderQueue.dequeue()`, `peek()`, and `getOrdersInServeOrder()` to let a much shorter order go ahead, with a skip counter on each order so a large one is skipped at most twice. An order that is already cooking is never skipped. *(Owner: Archer)*
2. **Tests for small orders first:** a small order served before a large one, a large order served after two skips, a cooking order never skipped, and rush orders still ahead of normal ones. *(Owner: Joshua)*
3. **Show the full line with waits** in the display. *(Owner: Prabhav, with Scott for `updateView()`)*
4. **Final README and reflection.** *(Owner: Scott)*

## Attribution

| File | Who worked on it | How |
|---|---|---|
| `Order.java` | Archer | Wrote; updated for the dish map in Prototype 2. Fixes in "Fixes all errors" by Scott with GenAI assistance. |
| `OrderQueue.java` | Archer | Wrote; updated for the dish map in Prototype 2. Fixes by Scott with GenAI assistance. |
| `OrderStatus.java` | Archer | Wrote |
| `Menu.java` | Joshua | Wrote |
| `WaitTimeEstimator.java` | Joshua | Wrote; fixed by Scott with GenAI assistance to use `getItems()` instead of parsing notes |
| `OrderStatistics.java` | Joshua | Wrote |
| `OrderMode.java` | Prabhav | Wrote |
| `OrderTrackerDisplay.java` | Prabhav | Wrote |
| `OrderTrackerView.java` | Prabhav | Wrote; menu prompts and End Shift option fixed by Scott with GenAI assistance |
| `OrderTrackerController.java` | Scott | Wrote with GenAI assistance, then revised to match the group's code |
| `Main.java` | Scott | Wrote with GenAI assistance |
| `OrderTest.java` | Joshua | Wrote; rewritten for the dish map by Scott with GenAI assistance |
| `OrderQueueTest.java` | Joshua | Wrote; rewritten for the dish map by Scott with GenAI assistance |
| `WaitTimeEstimatorTest.java` | Scott | Wrote with GenAI assistance |
| `OrderTrackerControllerTest.java` | Scott | Wrote with GenAI assistance |
| `README.md` | Scott | Drafted with GenAI assistance; reviewed by group |

### GenAI note

Scott used Claude to draft the controller, `Main`, `OrderTrackerControllerTest`, and this README, giving it the spec and the group's code so method names would match. In Prototype 2 he also used it to find and fix errors after the group's changes: `WaitTimeEstimator` was still reading the notes instead of the dish map, the view wasn't calling the new `handleAddOrder`, and the existing tests still used descriptions instead of dishes. It rewrote `OrderTest` and `OrderQueueTest` for the dish map and added `WaitTimeEstimatorTest`. Archer used GenAI to draft outlines for `Order`, `OrderQueue`, and `OrderStatus` before writing them. Joshua used GenAI to check that his tests matched the group's scope and spec. [Prabhav: add how you used GenAI, or say you didn't.]

---

# Prototype 1 (Deliverable 3)

## Reflection

### What changed from the spec

| Area | What the spec said | What we built | Why / what prompted it |
|---|---|---|---|
| Interface | A GUI with text fields, buttons, and a normal/rush toggle. | A console menu (`OrderTrackerView`) that reads input with a `Scanner`. | Group discussion when starting the view: a console is simpler to build and test, and the model and controller didn't need to change. |
| View/controller connection | The controller would mediate between model and view. | The view runs the menu loop and calls the controller's `handle...` methods. The controller sends updates back through the `OrderTrackerDisplay` interface (`updateDisplay(String, int)`, `showMessage(String)`). | Prabhav's view design. Using an interface means the controller only knows the two methods it needs, not the whole view. |
| Normal/rush input | `handleAddOrder(String, String, boolean rush)` | `handleAddOrder(String, String, OrderMode mode)`, using a new `OrderMode` enum (`NORMAL`, `RUSH`). A `null` mode means the user picked neither, and the controller shows "Please select a mode." | The console view asks the user to pick 1 or 2, so passing an enum was clearer than a boolean. |
| Controller constructor | Took the queue, statistics, wait-time estimator, and display. | Takes only `OrderQueue` and `OrderTrackerDisplay`. | `OrderStatistics` and `WaitTimeEstimator` were planned for Prototype 2. |
| Duplicate clicks | The controller tracked `lastAddTimeMillis` to ignore a double-clicked Add. | Removed. | A console has no double-clicks; each command is typed and entered once. |
| Validation | Table and description rules checked in both the controller and `Order.validate()`. | `validateInput` only checks that the typed table number is a whole number, then calls `Order.validate()` and shows its message. | A GenAI review found the controller's allowed description characters didn't match `Order`'s, so an input could pass one check and fail the other. Keeping the rules only in `Order` avoids that. |
| `OrderQueue` storage | One `deque<Order>`. | Two deques, `rushOrders` and `normalOrders`, plus a `nextOrderId` counter. | Inserting a rush order behind earlier rushes isn't possible with deque operations alone. Two deques keep every operation O(1). |
| Duplicate orders | Listed as a bad-input case with no response. | `enqueue` rejects an order if the same table already has an order with the same description waiting. | Catches an accidental second entry of the same order. |
| `OrderQueue` method names | Spec names: `enqueue`, `dequeue`, `peek`, `size`. | Both the spec names and the diagram names (`serveNext`, `getNextOrder`, `getWaitingCount`, `getQueue`) work. | The spec and the system diagram used different names, so both were kept. |
| Tests | Gave targets for Order and OrderQueue | Wrote down a list of requirements the app would have to pass to be functional as tests. | Making sure that the test written in code align with the targets specified in the classes |

### Did the model really stand alone?

Yes. `Order`, `OrderQueue`, `OrderStatus`, and their tests don't import or reference the view or the controller. The JUnit tests run without the view, and the model was finished and pushed before the view and controller were connected.

### What happened to the unknowns from the scope

| Unknown | Status |
|---|---|
| How to insert a rush order behind existing rushes in a deque | **Figured out.** Two deques: rush orders go to the back of `rushOrders`, and `rushOrders` is always served first. |
| How to track the most recent order so Cancel Last works with rushes | **Figured out.** Each order gets an increasing `orderId`. `cancelLast()` compares the backs of both deques and cancels the one with the higher ID. |
| How to store menu items and prep times and calculate waits | **Figured out in Prototype 2** with `Menu` and `WaitTimeEstimator`. |
| What end-of-shift stats should include and where to store them | **Figured out in Prototype 2** with `OrderStatistics` (total, rush, served, and cancelled counts). |
