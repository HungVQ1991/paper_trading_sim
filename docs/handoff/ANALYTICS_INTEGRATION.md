# Analytics integration package

**From:** Agent 3 (Analytics & Export), branch `feature/analytics`
**Date:** 2026-11-16
**State of the repo when written:** `main` 2049f06, `core-engine` 6511976,
`market-data` 44d3bb8, `frontend-update` 8e057a5

Contents:

1. Snippets for Agent 4 (`MainController`)
2. Architecture Change Requests (ACRs) for Agent 1 and Agent 5
3. Analytics checklist for Agent 5's E2E run

Every ACR below has the decision field `PENDING`; none is approved yet.

---

## 1. Snippets for Agent 4

Both snippets were compiled and passed `checkstyleMain` in a scratch checkout of
`frontend-update` that also contained `AnalyticsService`, `CsvExporter` and the reference
window from `docs/handoff/analytics-window/`. Neither was clicked through in a running
application.

### 1.1 `MainController.openAnalytics()`

Replace the two lines inside the `try` block:

```java
      AnalyticsController controller = loader.getController();
      controller.loadData(
          List.copyOf(account.getTradeHistory()), List.copyOf(account.getEquityHistory()));
      showChildWindow("Performance Analytics", root, 720, 560);
```

`List.copyOf` makes the snapshot explicit at the module boundary. It costs nothing today:
`Account` already returns `List.copyOf(...)` lists, and `List.copyOf` returns the same
instance when its argument is already an unmodifiable copy.

### 1.2 `MainController.exportTrades()` (recommended, optional)

The current method builds its own CSV: `\n` line endings, unformatted `double` values,
and the write happens on the FX thread. Delegating to `CsvExporter` gives the application a
single CSV format.

New imports:

```java
import com.trading.analytics.CsvExporter;
import java.util.concurrent.ExecutorService;
import javafx.concurrent.Task;
```

Method:

```java
  @FXML
  private void exportTrades() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export Trade History");
    chooser.setInitialFileName("trading_report.csv");
    chooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("CSV files", "*.csv"));
    java.io.File selectedFile = chooser.showSaveDialog(watchlistTable.getScene().getWindow());
    if (selectedFile == null) {
      return;
    }
    Path target = selectedFile.toPath();
    List<Trade> snapshot = List.copyOf(account.getTradeHistory());
    Task<Void> exportTask = new Task<>() {
      @Override
      protected Void call() throws IOException {
        CsvExporter.exportTrades(snapshot, target);
        return null;
      }
    };
    exportTask.setOnFailed(event -> showError(
        "Unable to export trades",
        "The report could not be saved. Check the folder and close the file if it is open."));
    ExecutorService exportExecutor = Executors.newSingleThreadExecutor();
    exportExecutor.execute(exportTask);
    exportExecutor.shutdown();
  }
```

After this change `csvValue(...)` and the imports `java.nio.charset.StandardCharsets` and
`java.nio.file.Files` may no longer be used in `MainController`; remove them if so.

---

## 2. Architecture Change Requests

### ACR-A3-01 — Base package and duplicated UI files (for Agent 5)

- **Current state.**
  - Docs and AGENTS.md §4.2 use `com.trading.model|engine|market|analytics|ui` and
    `com.trading.MainApp`.
  - `main` has `com.trading.sim.SimApplication`.
  - `frontend-update` puts all UI classes in `com.trading.sim.ui`, including `MainApp`,
    `SettingsController` and `AnalyticsController`.
  - `market-data` puts `SettingsController` in `com.trading.ui`.
  - `market-data` and `frontend-update` both add `fxml/SettingsView.fxml` and
    `css/dark-theme.css` with different content, and `core-engine` and `market-data` both
    add `com/trading/model/Quote.java` with different content.
- **Proposed change.** One UI package, `com.trading.ui`, as documented. Agent 5 names the
  single owner of `SettingsController`, `SettingsView.fxml`, `dark-theme.css` and `Quote`
  before the branches are merged.
- **Reason.** The branches cannot be merged cleanly as they stand, and two
  `SettingsController` classes would exist.
- **Affected.** Agent 4 (package move, `fx:controller` attributes, `application.mainClass`
  in `build.gradle`), Agent 2 (`SettingsController`, `Quote`), Agent 1 (`Quote`), Agent 5
  (`build.gradle`). Analytics is not affected: `com.trading.analytics` matches the docs.
- **Compatibility.** Mechanical rename; no behaviour change.
- **Decision:** PENDING. **Approver/date:** —

### ACR-A3-02 — Build toolchain (for Agent 5)

- **Current state.**
  - `build.gradle` sets `JavaLanguageVersion.of(22)`. AGENTS.md §1.2 and the architecture
    document §8.1 say Java 21 LTS.
  - `main` has no toolchain resolver in `settings.gradle`; `core-engine` adds
    `org.gradle.toolchains.foojay-resolver-convention` 1.0.0. On `main`, a machine without
    JDK 22 cannot build.
  - Gradle 9.7.1 does not start on JDK 27. Observed on Agent 3's machine:
    `BUG! exception in phase 'semantic analysis' ... Unsupported class file major version 71`.
    With `JAVA_HOME` pointing at JDK 21 the build succeeds (the resolver downloads JDK 22).
  - `build.gradle` uses Spring Boot 3.3.4 and Lombok; the architecture document §8.2 shows
    the plain `application` plugin with no Spring.
- **Proposed change.**
  1. Set the toolchain to 21, matching the documents:

     ```groovy
     java {
     	toolchain {
     		languageVersion = JavaLanguageVersion.of(21)
     	}
     }
     ```

  2. Merge the `settings.gradle` resolver from `core-engine` into `main`.
  3. Add a line to the README: "run Gradle with JDK 17–25 as `JAVA_HOME`".
  4. Decide whether Spring Boot and Lombok stay, and update either `build.gradle` or the
     architecture document so they agree.
- **Reason.** Reproducible builds for every team member; documents and build agree.
- **Affected.** Agent 5 (`build.gradle`, `settings.gradle`). `core-engine` uses pattern
  matching for `instanceof`, which Java 21 supports. Agent 3 has not built the project with
  toolchain 21; Agent 5 must run the full quality gate after the change.
- **Compatibility.** Analytics code uses nothing newer than Java 17.
- **Decision:** PENDING. **Approver/date:** —

### ACR-A3-03 — Record the `Trade` and equity-history contracts (for Agent 1 and Agent 5)

No code change is requested. This asks for decisions that are already implemented on
`core-engine` to be written down, because analytics depends on them.

- **Current state (observed in `core-engine`).**
  - `Trade` is immutable with getters `getTradeId()`, `getOrderId()`, `getSymbol()`,
    `isBuy()`, `getQuantity()`, `getExecutedPrice()`, `getRealizedPnL()`, `getTimestamp()`.
  - `TradingEngine.executeOrder` always records `realizedPnL = 0.0` for a BUY.
  - `Account` starts `equityHistory` with the initial cash, and `TradingEngine` appends
    `account.getTotalValue(latestPrices)` after every fill. `Account.reset` restarts the
    series.
  - `getTradeHistory()` and `getEquityHistory()` return `List.copyOf(...)` snapshots.
- **Proposed change.**
  1. Close ADR-003 in AGENTS.md §14 with the rule "one equity point at account creation
     (initial cash) and one after each fill, valued at the latest known prices".
  2. Add `getEquityHistory()`, `recordEquitySnapshot(double)` and the `Trade` getters to
     `docs/CORE_ENGINE.md` §4.1 and the architecture document §4.1.
  3. State in the `Trade` contract that BUY records carry `realizedPnL == 0`.
- **Reason.** `calculateMaxDrawdown` is only as meaningful as the sampling rule, and
  win rate / total P&L rely on the BUY rule.
- **Known limitation to record.** Equity is sampled only at fills, so a price drop between
  two fills is not seen by Max Drawdown.
- **Affected.** Agent 1 (docs only), Agent 5 (AGENTS.md §14), Agent 3 (no change).
- **Decision:** PENDING. **Approver/date:** —

### ACR-A3-04 — Synchronise the analytics specification (for Agent 5)

- **Current state.** The three documents disagree with each other and with AGENTS.md §5.4.
- **Proposed change.** Record the following, which is what `feature/analytics` implements
  and tests:

  | Topic | Rule implemented | Document to update |
  |---|---|---|
  | Best/worst trade (ADR-006) | `getBestTrade` / `getWorstTrade`, SELL trades only, `Optional.empty()` when none | Architecture document §4.4 |
  | Win-rate eligibility | Every SELL counts; a break-even SELL is a non-win | `docs/ANALYTICS.md` §3.1 (currently says `getRealizedPnL() != 0`) |
  | Best/worst tie-break | Earliest trade in list order | `docs/ANALYTICS.md` §3.1 |
  | Total P&L | Sum over SELL trades only | `docs/ANALYTICS.md` §3.1 |
  | Invalid input | Null list = empty; null element or non-finite equity value throws `IllegalArgumentException` | `docs/ANALYTICS.md` §3.1 |
  | Window entry point | `loadData(List<Trade>, List<Double>)` | `docs/FRONTEND.md` §4.3 (currently `initData(account.getTradeHistory())`) |
  | Window ownership | Agent 4 owns `AnalyticsController` and `AnalyticsView.fxml` | `docs/ANALYTICS.md` §2 and architecture document §7 (both assign them to Member 3) |
  | Monetary type (ADR-007) | `double` throughout analytics | AGENTS.md §14 |

- **Reason.** AGENTS.md §2.2 forbids silently resolving these; the project owner chose
  them on 2026-11-16 and they need a written home.
- **Affected.** Documentation only.
- **Decision:** PENDING. **Approver/date:** —

---

## 3. Analytics checklist for Agent 5's E2E run (AGENTS.md §8.2)

### 3.1 Scenario

The expected values below were produced by running exactly this sequence against
`TradingEngine` and `Account` from `core-engine` with `AnalyticsService` and `CsvExporter`
from `feature/analytics`.

Start: `new Account(100_000.0)`, one symbol `AAPL`.

| Step | Action | Fill | Equity after |
|---|---|---|---|
| 0 | Account created | — | 100,000.00 |
| 1 | Market BUY 100 at current price 100 | BUY 100 @ 100.00 | 100,000.00 |
| 2 | Limit BUY 50 @ 90 placed at price 100, then quote 88 | BUY 50 @ 88.00 | 98,800.00 |
| 3 | Market SELL 100 at current price 110 | SELL 100 @ 110.00, P&L +1,400.00 | 102,100.00 |
| 4 | Market SELL 50 at current price 90 | SELL 50 @ 90.00, P&L −300.00 | 101,100.00 |

### 3.2 Expected analytics

- [ ] Total trades: **4** (2 BUY, 2 SELL)
- [ ] Win rate: **50.00%** (1 winning SELL of 2)
- [ ] Net realized P&L: **$1,100.00**, shown with `profit-text`
- [ ] Max drawdown: **1.20%** (100,000 → 98,800)
- [ ] Best trade: the SELL with **+1,400.00**; worst trade: the SELL with **−300.00**
- [ ] Equity curve has **5 points**: 100000, 100000, 98800, 102100, 101100
- [ ] Final equity 101,100.00 equals initial cash plus net realized P&L

### 3.3 Expected CSV

- [ ] File starts with bytes `EF BB BF`
- [ ] First line is exactly
      `TradeID,Timestamp,Symbol,Action,Quantity,ExecutedPrice,RealizedPnL`
- [ ] 5 lines in total, each ending with CRLF
- [ ] Columns 3–7 of the four data rows, in order:

  ```text
  AAPL,BUY,100,100.00,0.00
  AAPL,BUY,50,88.00,0.00
  AAPL,SELL,100,110.00,1400.00
  AAPL,SELL,50,90.00,-300.00
  ```

- [ ] TradeID is a UUID and Timestamp matches `yyyy-MM-ddTHH:mm:ssZ` (no fractional seconds)
- [ ] The file opens in Excel with seven columns and correct decimals on a machine with a
      Vietnamese locale

### 3.4 Window behaviour

- [ ] Before any trade: all four tiles show zero values, the empty-state message is
      visible, and Export CSV is disabled
- [ ] After the scenario: tiles match 3.2 and the curve is drawn
- [ ] Export CSV, then Cancel in the dialog: nothing happens, no error
- [ ] Export CSV to a valid path: a success alert appears and the UI stays responsive
- [ ] Export CSV to a file that is open in Excel: an error alert appears with no stack trace
- [ ] After resetting the account from Settings and reopening Analytics: the empty state is
      shown again
- [ ] Closing the application right after an export leaves no running thread
