# cs2114-project3-group01

A console app for a restaurant kitchen. Servers add orders by table number and description, and the kitchen always works on the oldest order first. Rush orders are served ahead of normal orders, but stay first-in, first-out among themselves. The most recent order can be cancelled as long as the kitchen hasn't started cooking it.

## How to Run

**The app (thin view):** in Eclipse, open `src/Main.java` and choose **Run As → Java Application**. The menu appears in the Console panel; type a number and press Enter.

**The model on its own (tests):** right-click `OrderQueueTest.java` or `OrderTest.java` and choose **Run As → JUnit Test**. The tests create `Order` and `OrderQueue` objects directly and never open the view.

## Reflection

### What changed from the spec

| Area | What the spec said | What we built | Why / what prompted it |
|---|---|---|---|
| Interface | A GUI with text fields, buttons, and a normal/rush toggle. | A console menu (`OrderTrackerView`) that reads input with a `Scanner`. | Group discussion when starting the view: a console is simpler to build and test, and the model and controller didn't need to change. |
| View/controller connection | The controller would mediate between model and view. | The view runs the menu loop and calls the controller's `handle...` methods. The controller sends updates back through the `OrderTrackerDisplay` interface (`updateDisplay(String, int)`, `showMessage(String)`). | Prabhav's view design. Using an interface means the controller only knows the two methods it needs, not the whole view. |
| Normal/rush input | `handleAddOrder(String, String, boolean rush)` | `handleAddOrder(String, String, OrderMode mode)`, using a new `OrderMode` enum (`NORMAL`, `RUSH`). A `null` mode means the user picked neither, and the controller shows "Please select a mode." | The console view asks the user to pick 1 or 2, so passing an enum was clearer than a boolean. |
| Controller constructor | Took the queue, statistics, wait-time estimator, and display. | Takes only `OrderQueue` and `OrderTrackerDisplay`. | `OrderStatistics` and `WaitTimeEstimator` are planned for Prototype 2. |
| Duplicate clicks | The controller tracked `lastAddTimeMillis` to ignore a double-clicked Add. | Removed. | A console has no double-clicks; each command is typed and entered once. |
| Validation | Table and description rules checked in both the controller and `Order.validate()`. | `validateInput` only checks that the typed table number is a whole number, then calls `Order.validate()` and shows its message. | A GenAI review found the controller's allowed description characters didn't match `Order`'s, so an input could pass one check and fail the other. Keeping the rules only in `Order` avoids that. |
| `OrderQueue` storage | One `deque<Order>`. | Two deques, `rushOrders` and `normalOrders`, plus a `nextOrderId` counter. | Inserting a rush order behind earlier rushes isn't possible with deque operations alone. Two deques keep every operation O(1). |
| Duplicate orders | Listed as a bad-input case with no response. | `enqueue` rejects an order if the same table already has an order with the same description waiting. |  
| `OrderQueue` method names | Spec names: `enqueue`, `dequeue`, `peek`, `size`. | Both the spec names and the diagram names (`serveNext`, `getNextOrder`, `getWaitingCount`, `getQueue`) work. | The spec and the system diagram used different names, so both were kept. |
| Tests | Gave targets for Order and OrderQueue | Wrote down a list of requirements the app would have to pass to be functional as tests. | Making sure that the test written in code align with the targets specified in the classes |

### Did the model really stand alone?

Yes. `Order`, `OrderQueue`, `OrderStatus`, and their tests don't import or reference the view or the controller. The JUnit tests run without the view, and the model was finished and pushed before the view and controller were connected.

### What happened to the unknowns from the scope

| Unknown | Status |
|---|---|
| How to insert a rush order behind existing rushes in a deque | **Figured out.** Two deques: rush orders go to the back of `rushOrders`, and `rushOrders` is always served first. |
| How to track the most recent order so Cancel Last works with rushes | **Figured out.** Each order gets an increasing `orderId`. `cancelLast()` compares the backs of both deques and cancels the one with the higher ID. |
| How to store menu items and prep times and calculate waits | **Still open.** Planned for Prototype 2 with `WaitTimeEstimator`. |
| What end-of-shift stats should include and where to store them | **Partly figured out.** They'll live in an `OrderStatistics` model class (total, rush, served, and cancelled counts), but it isn't built yet. |

### What doesn't work yet

- No end-of-shift statistics or wait-time estimates.
- The display shows only the next order and the waiting count, not the full line.
- A table can't add a second identical order (for example, another "1 chicken") while the first is still waiting; the server has to word it differently.
- There are no automated tests for the controller yet. Bad input through the console was checked by hand.

## Next Steps

### Left to finish the MVP, in order

1. Run every bad-input case from the scope through the console and fix anything that crashes or gives the wrong message. *(Owner: Scott)*
2. Decide whether a table should be able to repeat the same order while the first is waiting, and update `enqueue` if so. *(Owner: Archer)*
3. Add a controller test using a fake `OrderTrackerDisplay` that records messages. 

### Non-trivial feature: wait-time estimation

Each waiting order will show an estimated time until it's ready, based on its own prep time and every order ahead of it.

**Predicted to change:**
- `WaitTimeEstimator` (new): `estimateWaitTime(Order)` and `getPrepTime(String)`.
- `OrderTrackerController`: the constructor (to take the estimator) and `updateView()` (to include the estimate).
- `OrderTrackerDisplay.updateDisplay(...)` and `OrderTrackerView.updateDisplay(...)`: a new parameter for the estimate.
- `Main.main(...)`: create the estimator and pass it to the controller.

**Should not change:** `Order`, `OrderStatus`, and `OrderQueue`. The estimator can read the line through `OrderQueue.getOrdersInServeOrder()`.

### Stretch goals

| Stretch goal | Status |
|---|---|
| Rush limits (rush orders served in the order they were rushed) | **Done** in Prototype 1. |
| Estimated wait times | **Committed** for Prototype 2. |
| End-of-shift stats | **Committed** for Prototype 2. |
| Small orders first (a short order can be served ahead of a much larger one, but a large order can be skipped at most twice so it never waits forever) | **Committed** for the final, after wait-time estimation, since it needs prep times to tell large orders from small ones. Changes `OrderQueue.dequeue()` and `OrderQueue.peek()`. |



### New tests needed

- `OrderTrackerControllerTest`: each `handle...` method with a fake display, for both normal input and bad input.
- `OrderStatisticsTest`: counts after adding, serving, and cancelling orders.
- `WaitTimeEstimatorTest`: estimates for an empty line, a line with rush orders, and unknown dishes.

### Owners for remaining items

| Item | Owner |
|---|---|
| `OrderStatistics` and `WaitTimeEstimator` | Joshua |
| Controller changes and `Main` | Scott |
| View changes for stats and wait times | Prabhav |
| Any `OrderQueue` changes | Archer |
| New tests | [TODO] |

## Attribution

| File | Who worked on it | How |
|---|---|---|
| `Order.java` | Archer | Drafted Outline with GenAI assistance, then wrote |
| `OrderQueue.java` | Archer | Drafted Outline with GenAI assistance, then wrote |
| `OrderStatus.java` | Archer | Drafted Outline with GenAI assistance, then wrote |
| `OrderMode.java` | Prabhav | Wrote |
| `OrderTrackerDisplay.java` | Prabhav | Wrote |
| `OrderTrackerView.java` | Prabhav | Wrote |
| `OrderTrackerController.java` | Scott | drafted with minimal GenAI assistance, then revised to match Archer's and Prabhav's code |
| `Main.java` | Scott | Wrote |
| `OrderQueueTest.java` | Joshua | Wrote, revised with GenAI Assistance|
| `OrderTest.java` | Joshua | Wrote, revised with GenAI Assistance |
| `README.md` | Scott | Drafted with GenAI assistance; reviewed by group. |

### GenAI note

Scott used Claude to draft this README. He also gave it the spec and the group's `Order`, `OrderQueue`, view code, and controller, and asked if there were any errors. It found that the controller didn't catch the duplicate-order exception from `enqueue` (which would have crashed the app) and that the controller's description rules didn't match `Order`'s, so validation was changed to call `Order.validate()`. Joshua used GenAI to ensure that the tests written were in line with the vision the group had during the scope and specification.
