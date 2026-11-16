# Basic JavaFX UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the initial JavaFX paper-trading dashboard and order-entry UI
without changing backend or build configuration.

**Architecture:** The new presentation layer stays under `com.trading.sim.ui`.
FXML provides the dashboard and order form; controllers populate only local
sample display data and delegate pure form rules to `OrderFormValidator`. No
class calls Spring, a trading engine, market services, analytics, or settings.

**Tech Stack:** Java 22, JavaFX 21 Controls/FXML, FXML, JavaFX CSS, JUnit 5,
Gradle, Checkstyle.

**Spec:** `docs/superpowers/specs/2026-09-24-basic-javafx-ui-design.md`

## Global Constraints

- Create or modify only the JavaFX UI layer, its test, its FXML/CSS resources,
  and this plan's tracked documentation.
- Do not modify `build.gradle`, `SimApplication`, Spring configuration, or
  existing backend modules.
- Use package root `com.trading.sim.ui` and 2-space Google-style indentation.
- Use explicit imports, no wildcard imports, and lines no longer than 100 chars.
- Keep dashboard data local and display-only until backend interfaces exist.
- Update future asynchronous JavaFX view changes through `Platform.runLater`.

## Review Focus

- Blank, zero, negative, decimal, and non-numeric quantities must not validate;
  Task 1 adds direct tests for each representative invalid quantity.
- A limit order with blank, zero, negative, or non-numeric price must not
  validate; Task 1 adds direct tests for those inputs.
- A market order must not require a limit price; Task 1 tests it explicitly.
- Estimated cost must format stable US currency for a valid decimal price;
  Task 1 tests the formatting result.
- FXML resources must load and reference existing controller handlers; Tasks 2
  and 3 compile the Java sources and use the JavaFX application manual smoke
  check after the Gradle suite.

---

## File Structure

- `src/main/java/com/trading/sim/ui/OrderFormValidator.java`: Pure validation
  and currency-formatting rules for order entry.
- `src/test/java/com/trading/sim/ui/OrderFormValidatorTest.java`: JUnit tests
  for all supported form input states.
- `src/main/java/com/trading/sim/ui/MainApp.java`: JavaFX entry point that
  loads the main FXML view and stylesheet.
- `src/test/java/com/trading/sim/ui/UiResourceTest.java`: Confirms FXML
  resources exist and are well-formed XML without requiring a display server.
- `src/main/java/com/trading/sim/ui/MainController.java`: Populates dashboard
  sample data, redraws the selected quote chart, and opens the order dialog.
- `src/main/java/com/trading/sim/ui/OrderDialogController.java`: Coordinates
  order-form controls with `OrderFormValidator`.
- `src/main/resources/fxml/MainView.fxml`: Dashboard layout.
- `src/main/resources/fxml/OrderDialog.fxml`: Modal order-entry layout.
- `src/main/resources/css/dark-theme.css`: Dark trading-terminal visual style.

### Task 1: Create the pure order-form validation contract

**Files:**

- Create: `src/test/java/com/trading/sim/ui/OrderFormValidatorTest.java`
- Create: `src/main/java/com/trading/sim/ui/OrderFormValidator.java`

**Interfaces:**

- Produces: `OrderFormValidator.validate(String orderType, String quantity,
  String limitPrice)` returning `ValidationResult` with `isValid()` and
  `message()`.
- Produces: `OrderFormValidator.formatEstimatedCost(String quantity,
  double unitPrice)` returning a US-currency `String`.
- Consumed by: `OrderDialogController` in Task 3.

- [ ] **Step 1: Write the failing validation tests**

```java
@Test
void rejectsInvalidQuantities() {
  assertFalse(validator.validate("MARKET", "", "").isValid());
  assertFalse(validator.validate("MARKET", "0", "").isValid());
  assertFalse(validator.validate("MARKET", "-5", "").isValid());
  assertFalse(validator.validate("MARKET", "1.5", "").isValid());
  assertFalse(validator.validate("MARKET", "shares", "").isValid());
}

@Test
void acceptsMarketOrderWithoutLimitPrice() {
  assertTrue(validator.validate("MARKET", "10", "").isValid());
}

@Test
void rejectsInvalidLimitPrice() {
  assertFalse(validator.validate("LIMIT", "10", "").isValid());
  assertFalse(validator.validate("LIMIT", "10", "0").isValid());
  assertFalse(validator.validate("LIMIT", "10", "-1").isValid());
  assertFalse(validator.validate("LIMIT", "10", "abc").isValid());
}

@Test
void formatsEstimatedCost() {
  assertEquals("$1,500.00", validator.formatEstimatedCost("10", 150.0));
}
```

- [ ] **Step 2: Run the test to verify RED**

Run: `./gradlew test --tests com.trading.sim.ui.OrderFormValidatorTest`

Expected: FAIL because `OrderFormValidator` does not yet exist.

- [ ] **Step 3: Implement the smallest pure validator**

```java
public ValidationResult validate(String orderType, String quantity, String limitPrice) {
  if (!isPositiveWholeNumber(quantity)) {
    return new ValidationResult(false, "Quantity must be a positive whole number.");
  }
  if ("LIMIT".equals(orderType) && !isPositiveNumber(limitPrice)) {
    return new ValidationResult(false, "Limit price must be greater than zero.");
  }
  return new ValidationResult(true, "");
}
```

Implement `formatEstimatedCost` with `NumberFormat.getCurrencyInstance(Locale.US)`
and a nested immutable `ValidationResult` record.

- [ ] **Step 4: Run the focused test to verify GREEN**

Run: `./gradlew test --tests com.trading.sim.ui.OrderFormValidatorTest`

Expected: PASS with 4 tests and zero failures.

- [ ] **Step 5: Commit the validation unit**

```bash
git add src/main/java/com/trading/sim/ui/OrderFormValidator.java \
  src/test/java/com/trading/sim/ui/OrderFormValidatorTest.java
git commit -m "feat: add order form validation"
```

### Task 2: Create the dashboard shell and styling

**Files:**

- Create: `src/main/java/com/trading/sim/ui/MainApp.java`
- Create: `src/main/java/com/trading/sim/ui/MainController.java`
- Create: `src/main/resources/fxml/MainView.fxml`
- Create: `src/main/resources/css/dark-theme.css`
- Create: `src/test/java/com/trading/sim/ui/UiResourceTest.java`

**Interfaces:**

- Consumes: none from the production backend; dashboard sample data remains
  controller-private.
- Produces: `MainApp.start(Stage stage)` that loads `/fxml/MainView.fxml` and
  applies `/css/dark-theme.css`.
- Produces: `MainController.openOrderDialog()` handler consumed by the main
  FXML button.
- Consumed by: Task 3's `/fxml/OrderDialog.fxml` and `OrderDialogController`.

- [ ] **Step 1: Write the failing FXML resource test**

```java
@Test
void mainViewIsWellFormedXml() throws Exception {
  assertXmlResourceIsWellFormed("/fxml/MainView.fxml");
}

private void assertXmlResourceIsWellFormed(String resourcePath) throws Exception {
  try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
    assertNotNull(stream, resourcePath + " must exist");
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
  }
}
```

- [ ] **Step 2: Run the resource test to verify RED**

Run: `./gradlew test --tests com.trading.sim.ui.UiResourceTest`

Expected: FAIL because `/fxml/MainView.fxml` does not yet exist.

- [ ] **Step 3: Implement the dashboard shell**

Use a `BorderPane`: menu bar at top; summary cards and provider status under
it; a watchlist table and `LineChart` in the center; positions, pending orders,
and history tables inside a bottom `TabPane`. Populate the watchlist with AAPL,
MSFT, and TSLA sample rows. Selecting a row refreshes a 30-point sample series
and chart title. Add the documented dark colors and buy/sell/profit/loss CSS
classes. `MainApp` sets the title to `Paper Trading Simulator - Realtime System`.

Define a temporary `openOrderDialog()` method that displays a local informational
alert; Task 3 replaces it with modal dialog loading.

- [ ] **Step 4: Run the resource test and compilation to verify GREEN**

Run: `./gradlew test --tests com.trading.sim.ui.UiResourceTest classes`

Expected: PASS with the resource test green and `classes` exit code 0.

- [ ] **Step 5: Commit the dashboard shell**

```bash
git add src/main/java/com/trading/sim/ui/MainApp.java \
  src/main/java/com/trading/sim/ui/MainController.java \
  src/main/resources/fxml/MainView.fxml \
  src/main/resources/css/dark-theme.css \
  src/test/java/com/trading/sim/ui/UiResourceTest.java
git commit -m "feat: add trading dashboard ui"
```

### Task 3: Add the modal order-entry dialog and connect it

**Files:**

- Create: `src/main/java/com/trading/sim/ui/OrderDialogController.java`
- Create: `src/main/resources/fxml/OrderDialog.fxml`
- Modify: `src/main/java/com/trading/sim/ui/MainController.java`
- Modify: `src/test/java/com/trading/sim/ui/UiResourceTest.java`

**Interfaces:**

- Consumes: `OrderFormValidator.validate` and `formatEstimatedCost` from Task 1.
- Consumes: `MainController.openOrderDialog()` from Task 2.
- Produces: `OrderDialogController.initialize()` and `confirmOrder()` handlers
  referenced from `OrderDialog.fxml`.
- Produces: a modal dialog opened by `MainController.openOrderDialog()`.

- [ ] **Step 1: Extend the failing resource test for the order dialog**

```java
@Test
void orderDialogIsWellFormedXml() throws Exception {
  assertXmlResourceIsWellFormed("/fxml/OrderDialog.fxml");
}
```

- [ ] **Step 2: Run the resource test to verify RED**

Run: `./gradlew test --tests com.trading.sim.ui.UiResourceTest`

Expected: FAIL because `/fxml/OrderDialog.fxml` does not yet exist.

- [ ] **Step 3: Implement dialog behavior**

`initialize()` loads symbols `AAPL`, `MSFT`, and `TSLA`, sets `MARKET`, selects
Buy, disables the limit-price field, and registers listeners to recompute cost.
`updateOrderType()` enables limit price only for `LIMIT`. `confirmOrder()` uses
the validator: invalid input updates the error label; valid input displays a
confirmation using the formatted estimate and closes only when the user presses
Cancel or Close. Replace Task 2's placeholder with an `FXMLLoader`, owned
`Stage`, `initModality(Modality.WINDOW_MODAL)`, and `showAndWait()`.

- [ ] **Step 4: Run the dialog resource test and compilation to verify GREEN**

Run: `./gradlew test --tests com.trading.sim.ui.UiResourceTest classes`

Expected: PASS with both FXML resources well-formed and `classes` exit code 0.

- [ ] **Step 5: Perform the manual JavaFX smoke check**

Run `MainApp` from the IDE. Confirm that the dashboard opens, selecting AAPL,
MSFT, and TSLA changes the chart heading, Place Order opens a modal dialog,
MARKET disables the price field, LIMIT enables it, invalid input stays visible,
and valid input produces a confirmation.

- [ ] **Step 6: Commit the dialog integration**

```bash
git add src/main/java/com/trading/sim/ui/MainController.java \
  src/main/java/com/trading/sim/ui/OrderDialogController.java \
  src/main/resources/fxml/OrderDialog.fxml
git commit -m "feat: add order entry dialog"
```

### Task 4: Perform final UI-only verification

**Files:**

- Modify: no production files.

**Interfaces:**

- Consumes: all interfaces from Tasks 1-3.
- Produces: final verification evidence only.

- [ ] **Step 1: Run the full automated verification**

Run: `./gradlew test checkstyleMain`

Expected: PASS, including the existing Spring context test and the order-form
validation tests, with zero Checkstyle violations.

- [ ] **Step 2: Inspect the scoped diff**

Run: `git diff --check HEAD~3..HEAD` and `git diff --name-only HEAD~3..HEAD`

Expected: no whitespace errors; only the UI classes/resources and test from
this plan are present.

- [ ] **Step 3: Commit no further code**

Leave the verified commits unchanged; do not alter backend or build files.
