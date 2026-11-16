# Basic JavaFX UI Design

## Goal

Create the initial desktop presentation layer for the paper-trading simulator.
The result is a dark, usable trading dashboard with a working order-entry dialog
and local sample data. It must not modify Spring Boot startup, Gradle setup, or
the unfinished core, market-data, and analytics modules.

## Scope

The UI module will contain:

- `MainApp` to load the main FXML view and stylesheet.
- `MainView.fxml` and `MainController` for the dashboard.
- `OrderDialog.fxml` and `OrderDialogController` for order entry.
- `dark-theme.css` for the documented trading-terminal visual system.
- A small validation class and JUnit tests for order-form rules.

The dashboard includes the documented menu bar, account-summary cards, provider
status, watchlist, price chart, positions, pending orders, and trade history.
The new-order button opens the order dialog. Sample UI-only records populate the
dashboard until the future engine and market services are available.

## Architecture

All Java classes live under `com.trading.sim.ui`, respecting the repository's
existing package root. FXML and CSS reside in `src/main/resources/fxml` and
`src/main/resources/css`.

`MainApp` loads the dashboard with `FXMLLoader` and applies `dark-theme.css`.
`MainController` owns display-only sample data and opens the modal order dialog.
`OrderDialogController` delegates input checks and cost formatting to a
framework-independent validator so validation can be tested without starting a
JavaFX toolkit.

No controller imports or calls Spring, `TradingEngine`, `MarketService`,
analytics, or settings code. Later integrations can replace the local sample
data and invoke existing controllers through explicit initialization methods.

## User Interactions

- Selecting a watchlist row updates the chart heading and its sample series.
- Pressing **Place Order** opens a modal dialog.
- Choosing `MARKET` disables the limit-price field; choosing `LIMIT` enables it.
- Valid orders show their estimated cost and a confirmation message. The dialog
  does not create backend orders in this pass.
- Invalid quantity or limit-price input remains in the dialog with a clear error.

## Error Handling and Threading

FXML-loading failures are surfaced as an unrecoverable launch error. Form input
errors stay local to the dialog. The first pass does not receive asynchronous
market data, so it has no background UI updates. Future quote listeners must
marshal view updates with `Platform.runLater`.

## Verification

JUnit tests cover positive quantity, limit-order price requirements, and
estimated-cost formatting. Gradle tests, main-source compilation, and Checkstyle
will be run after the implementation.
