# AGENTS.md

## Multi-Agent Specification & Coordination Directive

### Project: Real-Time Paper Trading Simulation System

**Document version:** 2.0\
**Status:** Development baseline\
**Primary architecture reference:** `SYSTEM_ARCHITECTURE_DOCUMENT.md`\
**Target runtime:** Java 21 LTS + JavaFX 21\
**Build system:** Gradle Wrapper (repository-pinned version)

------------------------------------------------------------------------

## 1. Purpose and Operating Principles

This document defines how AI agents collaborate to implement, integrate, test, and maintain the Paper Trading Simulation System.

It is an execution and coordination directive. It does not replace the system architecture document and must not silently introduce architectural decisions that conflict with it.

All agents MUST follow these principles:

1.  **Architecture before implementation:** understand the relevant architecture and contracts before editing code.
2.  **Single ownership:** each source file has one owning implementation agent.
3.  **Contract-first parallelism:** shared interfaces and data contracts are agreed before dependent implementation begins.
4.  **Small, verifiable changes:** implement the smallest coherent change and verify it before reporting completion.
5.  **No fabricated verification:** never claim a command, test, build, or runtime workflow passed unless it was actually executed and its result observed.
6.  **Explicit uncertainty:** document assumptions, blockers, and unresolved decisions instead of silently guessing.
7.  **Offline-first tests:** unit and deterministic integration tests must not require live market APIs, network access, or real API keys.

### 1.1 Project Summary

The project is a standalone desktop Paper Trading application implemented in Java and JavaFX. It simulates stock trading using real-time market data from supported providers and an embedded offline Mock engine.

Core capabilities:

-   Market and limit orders.
-   Account cash and position management.
-   Real-time quote ingestion and watchlist updates.
-   Runtime market-provider switching.
-   Portfolio analytics and CSV trade export.
-   JavaFX dashboard, order dialog, analytics view, and provider settings.
-   Automated unit, integration, and end-to-end verification.

### 1.2 Technology Baseline

  Concern     Project baseline
  ----------- ------------------------------------------------------------
  Java        JDK 21 LTS
  JavaFX      OpenJFX 21.x, compatible with the repository configuration
  Build       Gradle Wrapper committed to the repository
  WebSocket   Java-WebSocket 1.5.6
  JSON        Jackson Databind 2.17.0
  Tests       JUnit 5 and Mockito
  Style       Google Java Style / repository Checkstyle configuration
  Date/time   `java.time`
  REST        JDK `java.net.http.HttpClient`
  CSV         JDK I/O APIs, RFC 4180-compatible output

The Gradle Wrapper is the authoritative Gradle version. Agents MUST use `./gradlew` (or `gradlew.bat` on Windows), not a locally installed Gradle version.

Do not change Java, JavaFX, Gradle, or dependency versions without approval from Agent 5 and an accompanying architecture/configuration update.

------------------------------------------------------------------------

## 2. Documentation Authority and Conflict Resolution

### 2.1 Source-of-Truth Rules

Use the following authority model:

1.  Explicit, latest user-approved project decisions.
2.  Approved `SYSTEM_ARCHITECTURE_DOCUMENT.md` for system behavior, architecture, domain rules, and intended scope.
3.  This `AGENTS.md` for agent roles, file ownership, workflow, quality gates, and implementation coordination.
4.  Existing source code and build configuration, as evidence of the current implementation---not automatic authority over approved requirements.
5.  Agent assumptions, which are never authoritative until documented and approved.

If this document specifies an implementation contract more precisely than the architecture document, agents must follow the contract for implementation while reporting any architectural gap to Agent 5. Agent 5 coordinates an approved update to the architecture document where needed.

### 2.2 Conflict Handling

Agents MUST NOT silently resolve conflicts involving:

-   Public method signatures or shared DTOs.
-   Package boundaries or dependency direction.
-   Java/JDK/JavaFX/Gradle versions.
-   Financial calculations or order-execution rules.
-   Provider capabilities, lifecycle, or API behavior.
-   File ownership or shared configuration.
-   Persistence, security, or account-state semantics.

When a conflict is found:

1.  Stop work only on the affected decision or dependent implementation.
2.  Report the conflicting statements and their exact document sections to Agent 5.
3.  State the impact and provide a recommended resolution.
4.  Wait for an explicit decision when the issue changes behavior or a shared contract.
5.  Update the relevant contract/documentation before dependent agents proceed.

Do not block unrelated work that can safely continue against an already agreed contract.

### 2.3 Architecture Change Control

Any change to a domain model, public interface, package boundary, provider behavior, financial formula, or shared file contract requires an Architecture Change Request (ACR).

An ACR must include:

-   Current behavior and relevant source/document references.
-   Proposed change and reason.
-   Affected classes, files, agents, and tests.
-   Backward-compatibility or migration impact.
-   Decision: approved, rejected, or deferred.
-   Approver and date/version.

Agent 5 coordinates the request. Only an explicit user-approved project decision or an approval delegated by the user may authorize a material architecture change.

------------------------------------------------------------------------

## 3. Multi-Agent Organization

The project uses four implementation agents and one integration/QA coordination role.

Agent 5 is a distinct coordination role; it does not imply that the project has a fifth human developer. The four implementation agents correspond to the four development workstreams in the architecture document.

``` mermaid
graph TD
    Lead["Agent 5: Lead Integrator & QA"]
    A1["Agent 1: Core Domain & Trading Engine"]
    A2["Agent 2: Market Data & Provider Adapters"]
    A3["Agent 3: Analytics & Export"]
    A4["Agent 4: JavaFX UI/UX"]

    Lead --> A1
    Lead --> A2
    Lead --> A3
    Lead --> A4

    A2 -->|"QuoteListener / Quote"| A1
    A1 -->|"TradingEngine / Account contracts"| A4
    A1 -->|"Trade / Equity history"| A3
    A2 -->|"Provider manager contracts"| A4
    A3 -->|"Analytics / CSV contracts"| A4
```

### 3.1 Agent 1 --- Core Domain & Trading Engine

**Owns:**

-   `src/main/java/com/trading/model/**`
-   `src/main/java/com/trading/engine/**`
-   Corresponding tests under `src/test/java/com/trading/model/**` and `engine/**`

**Responsibilities:**

-   Implement domain entities, order hierarchy, account, positions, quotes, and trade records.
-   Implement order validation, execution, pending-order lifecycle, and account mutations.
-   Consume market updates through the agreed `QuoteListener` contract.
-   Keep domain and engine code independent of JavaFX, provider implementations, and UI controllers.
-   Provide stable APIs to Analytics and UI agents.

### 3.2 Agent 2 --- Market Data & Provider Adapters

**Owns:**

-   `src/main/java/com/trading/market/**`
-   Market adapter and parser tests under `src/test/java/com/trading/market/**`
-   Market-specific configuration defaults/templates that contain no secrets.

**Responsibilities:**

-   Define and implement `MarketService`, `QuoteListener`, provider types, and provider manager.
-   Implement Twelve Data, Finnhub, and Mock adapters.
-   Implement Alpha Vantage only according to the approved provider scope and verified API capability. The architecture document lists Alpha Vantage, while the original task allocation did not assign its adapter explicitly; Agent 5 must confirm the release scope before this adapter is treated as a release requirement.
-   Normalize provider messages into the common `Quote` model.
-   Manage subscription replay, connection lifecycle, reconnection, and provider-switch failure reporting.
-   Never modify JavaFX controllers or FXML files.

### 3.3 Agent 3 --- Analytics & Export

**Owns:**

-   `src/main/java/com/trading/analytics/**`
-   `src/test/java/com/trading/analytics/**`

**Responsibilities:**

-   Implement win rate, total P&L, maximum drawdown, best trade, and worst trade where included in the approved contract.
-   Implement RFC 4180-compatible CSV export.
-   Keep analytics independent of JavaFX and market-provider implementations.
-   Use immutable input snapshots or defensive copies when consuming account history.
-   Document calculation definitions and edge-case behavior.

### 3.4 Agent 4 --- JavaFX UI/UX

**Owns:**

-   `src/main/java/com/trading/MainApp.java`
-   `src/main/java/com/trading/ui/**`
-   `src/main/resources/fxml/**`
-   `src/main/resources/css/**`
-   UI tests under `src/test/java/com/trading/ui/**`

**Responsibilities:**

-   Implement the dashboard, order dialog, analytics view, settings view, and their controllers.
-   Own all JavaFX presentation files, including `SettingsController.java` and `SettingsView.fxml`.
-   Integrate only through published service/interface contracts.
-   Dispatch all JavaFX control mutations to the JavaFX Application Thread.
-   Avoid blocking I/O and heavy calculations on the JavaFX Application Thread.
-   Keep view logic separate from financial business rules.

### 3.5 Agent 5 --- Lead Integrator & QA

**Owns:**

-   Integration coordination and approved shared configuration changes.
-   `build.gradle`, Gradle Wrapper updates, and `config/checkstyle/**` unless explicitly delegated.
-   Integration/E2E test scaffolding not assigned to a feature agent.
-   This document's coordination sections, subject to user approval for architectural changes.

**Responsibilities:**

-   Resolve contract conflicts and coordinate ACRs.
-   Maintain the file ownership map and RACI matrix.
-   Verify build configuration and dependency compatibility.
-   Integrate completed work and run quality gates.
-   Reject unverified completion claims.
-   Coordinate cross-module and E2E testing.
-   Keep the project buildable at integration checkpoints.

Agent 5 must not casually rewrite an implementation agent's module. If a fix belongs to an owned module, return it to that owner unless an urgent integration fix is explicitly delegated.

------------------------------------------------------------------------

## 4. File Ownership and Change Boundaries

### 4.1 Ownership Rules

1.  Every source file has exactly one owning implementation agent.
2.  An agent may read any project file needed to understand dependencies.
3.  An agent MUST NOT edit a file owned by another agent without prior approval from Agent 5.
4.  Shared contracts are changed by the owning agent only after affected agents agree and Agent 5 records the decision.
5.  If a task requires cross-owner edits, split the task into coordinated changes or explicitly delegate ownership.
6.  Do not overwrite, regenerate, or revert another agent's uncommitted work.
7.  Do not make broad formatting or refactoring changes outside the task scope.

### 4.2 File Ownership Matrix

  ----------------------------------------------------------------------------
  Artifact                     Owner                   Consulted
  ---------------------------- ----------------------- -----------------------
  `com.trading.model/**`       Agent 1                 Agents 2, 3, 4

  `com.trading.engine/**`      Agent 1                 Agents 2, 4

  `com.trading.market/**`      Agent 2                 Agents 1, 4

  `com.trading.analytics/**`   Agent 3                 Agents 1, 4

  `com.trading/MainApp.java`   Agent 4                 Agents 2, 5

  `com.trading.ui/**`          Agent 4                 Agents 1, 2, 3

  `resources/fxml/**`          Agent 4                 Agents 2, 3

  `resources/css/**`           Agent 4                 ---

  `config/checkstyle/**`       Agent 5                 All

  `build.gradle`, Gradle       Agent 5                 All
  Wrapper                                              

  Cross-module integration     Agent 5, unless         Relevant owners
  tests                        delegated               
  ----------------------------------------------------------------------------

This resolves the previous ambiguity: Agent 2 owns market code; Agent 4 owns all JavaFX UI code, including Settings.

------------------------------------------------------------------------

## 5. Shared Interface Contracts

The following contracts are the integration baseline. If existing code differs, the owning agent must report the discrepancy before changing either side.

### 5.1 Core Domain Contracts --- Agent 1

Expected domain types:

-   `Order` (abstract)
-   `MarketOrder`
-   `LimitOrder`
-   `OrderStatus`
-   `Position`
-   `Account`
-   `Quote`
-   `Trade`

Required behavior:

-   `Order.canExecute(double currentPrice)` is polymorphic.
-   `MarketOrder.canExecute(...)` returns `true` for a valid market price.
-   `LimitOrder` BUY is executable when `currentPrice <= limitPrice`.
-   `LimitOrder` SELL is executable when `currentPrice >= limitPrice`.
-   `Position` supports weighted-average-cost updates, partial sales, realized P&L, unrealized P&L, and market value.
-   `Trade` is an immutable record of an executed trade.
-   Public collection accessors must return immutable views or defensive copies.

The existing architecture uses `double` for prices and monetary values. Agents MUST preserve that public contract unless a BigDecimal migration is explicitly approved through an ACR. Do not mix `double` and `BigDecimal` monetary calculations opportunistically.

### 5.2 Trading Engine Contract --- Agent 1

The UI-facing baseline is:

``` java
boolean placeOrder(Order order, double currentPrice);
boolean cancelOrder(String orderId);
List<Order> getPendingOrders();
```

The engine consumes market events through:

``` java
void onPriceUpdate(Quote quote);
```

The account-facing baseline includes:

``` java
double getCashBalance();
double getTotalValue(Map<String, Double> currentPrices);
Map<String, Position> getPositions();
List<Trade> getTradeHistory();
List<Double> getEquityHistory();
```

Rules:

-   `placeOrder` must validate the order before mutating account state.
-   A rejected order must not partially mutate cash, positions, or trade history.
-   An order must not be filled more than once.
-   A cancelled or rejected order must never later become filled.
-   A pending order must be removed or transitioned exactly once when filled or cancelled.
-   Returned collections must not expose mutable internal state.
-   `getEquityHistory()` must return a stable snapshot. Its sampling/recording policy is an architecture decision that Agent 5 must resolve with Agent 1 before implementation relies on it.

**Open financial decision --- pending-order reservations:** The architecture document does not fully define whether pending BUY orders reserve cash or pending SELL orders reserve shares. The safe behavior is to prevent aggregate pending orders from overspending available cash or overselling available shares. Agent 1 must propose a reservation policy and tests; Agent 5 must obtain approval and record the rule before implementing final reservation semantics. Do not silently choose a policy.

### 5.3 Market Data Contracts --- Agent 2

The provider abstraction must support the architecture's common operations:

``` java
void start();
void stop();
void subscribe(String symbol);
void unsubscribe(String symbol);
Quote getLatestQuote(String symbol);
void addListener(QuoteListener listener);
void removeListener(QuoteListener listener);
MarketProviderType getProviderType();
```

`QuoteListener` must provide a common quote callback, consistent with:

``` java
void onPriceUpdate(Quote quote);
```

Provider manager baseline:

``` java
void registerProvider(MarketProviderType type, MarketService service);
void switchProvider(MarketProviderType newType, String apiKey);
void addGlobalListener(QuoteListener listener);
void subscribe(String symbol);
MarketService getActiveService();
MarketProviderType getActiveProviderType();
```

If implementation requires return values, asynchronous completion, or error callbacks beyond this baseline, Agent 2 must propose the change to Agent 5 and notify Agents 1 and 4 before implementation.

Provider switching requirements:

1.  Validate the target provider and required credentials before disrupting a healthy provider where possible.
2.  Stop the old provider safely.
3.  Start the target provider.
4.  Restore listeners and subscriptions.
5.  Publish connection state and any failure to the UI-facing observer.
6.  Do not report a successful switch until the new provider reaches its defined connected/ready state.
7.  Define and test the failure path. If target startup fails, report the failure and use the approved fallback/rollback policy; do not leave the UI falsely showing CONNECTED.

The provider manager owns global listener and symbol registries. Adapters own their transport-specific connection and parsing logic.

### 5.4 Analytics Contracts --- Agent 3

Baseline methods:

``` java
double calculateWinRate(List<Trade> trades);
double calculateTotalPnL(List<Trade> trades);
double calculateMaxDrawdown(List<Double> equityHistory);
Optional<Trade> getBestTrade(List<Trade> trades);
Optional<Trade> getWorstTrade(List<Trade> trades);
void exportTrades(List<Trade> trades, Path targetPath) throws IOException;
```

The best/worst trade methods are included in the AGENTS implementation scope; Agent 5 must ensure the architecture document is updated to include them.

Calculation definitions:

-   Win Rate: winning closed SELL trades divided by all closed SELL trades, multiplied by 100. Return `0.0` when there are no closed SELL trades.
-   Total P&L: sum realized P&L according to the agreed `Trade` semantics. Do not count BUY-side records as realized wins/losses unless their data model explicitly represents realized P&L.
-   Maximum Drawdown: maximum peak-to-trough percentage decline in the supplied equity series. Return `0.0` for fewer than two points or when no valid positive peak exists.
-   Best/Worst Trade: compare realized P&L among closed SELL trades; return `Optional.empty()` when no eligible trade exists.

CSV requirements:

-   UTF-8 output with BOM (`\uFEFF`) for spreadsheet compatibility.
-   RFC 4180-compatible quoting and escaping.
-   Stable header order: `TradeID,Timestamp,Symbol,Action,Quantity,ExecutedPrice,RealizedPnL`.
-   Correctly escape commas, quotes, CR/LF, and Unicode.
-   Propagate file errors using `IOException`; do not swallow exceptions.
-   Tests must use temporary directories and clean up generated files.

### 5.5 UI Integration Contracts --- Agent 4

The UI consumes contracts from Agents 1--3 and must not duplicate business logic.

Required views:

-   `MainView.fxml`
-   `OrderDialog.fxml`
-   `AnalyticsView.fxml`
-   `SettingsView.fxml`

Required dashboard elements:

-   Cash balance, total equity, and P&L summary.
-   Provider selector and visible provider connection status.
-   Watchlist with symbol, current price, and percentage change.
-   Price chart capped at the latest 30 points.
-   Tabs for positions, pending orders, and executed trade history.

Order dialog:

-   Symbol, side, order type, quantity, and limit price when applicable.
-   Disable limit-price input for MARKET orders.
-   Show estimated order cost.
-   Validate user input and display actionable errors.
-   Delegate final financial validation to TradingEngine.

Settings:

-   Provider selection and API-key input.
-   API keys must be masked in the UI.
-   No real secrets in test fixtures, source code, or committed configuration.

------------------------------------------------------------------------

## 6. Universal Engineering Invariants

All agents must follow these rules.

### 6.1 Google Checkstyle and Code Style

The repository's `config/checkstyle/google_checks.xml` is authoritative.

-   Zero Checkstyle warnings.
-   `maxWarnings = 0`.
-   `ignoreFailures = false`.
-   Two spaces per indentation level; tabs are forbidden.
-   Maximum line length: 100 characters, subject to the configured Checkstyle rule.
-   No wildcard imports.
-   K&R braces; always use braces for control structures.
-   Packages lowercase; types UpperCamelCase; methods/variables lowerCamelCase; constants UPPER_SNAKE_CASE.
-   Avoid unused imports, dead code, unexplained magic numbers, and unnecessary suppression annotations.
-   Do not weaken Checkstyle rules to make a failing change pass without Agent 5 approval.

### 6.2 Architecture Boundaries

-   `com.trading.model`, `com.trading.engine`, and `com.trading.analytics` MUST NOT import JavaFX classes.
-   Domain and engine code MUST NOT depend on concrete provider adapters.
-   UI code may depend on public application/service contracts, not provider transport internals.
-   Market adapters MUST NOT depend on UI controllers or FXML.
-   Avoid circular package dependencies.
-   Depend on interfaces at module boundaries.
-   Keep DTOs and domain models independent of Jackson annotations unless explicitly justified and approved.

### 6.3 Encapsulation and Immutability

-   Fields must be `private`, using `final` wherever practical.
-   Do not expose mutable internal collections.
-   Use immutable snapshots, defensive copies, or unmodifiable views.
-   Prefer immutable value objects for quote and trade data.
-   Validate constructor arguments and state transitions.
-   Do not return `null` where an empty collection or `Optional` is appropriate.

### 6.4 Concurrency and JavaFX Threading

-   WebSocket callbacks and mock market generation run off the JavaFX Application Thread.
-   All JavaFX control and scene-graph mutations MUST occur on the JavaFX Application Thread, using `Platform.runLater()` when called from a background thread.
-   Never block the JavaFX Application Thread with network I/O, file I/O, long computations, or `Thread.sleep()`.
-   Use managed executors and shut them down during application/provider shutdown.
-   Avoid creating unmanaged threads per quote or per UI update.
-   Shared mutable state must have an explicit concurrency policy.
-   Use concurrent collections or synchronization only where they solve a documented access pattern; thread-safe collections alone do not make compound business operations atomic.
-   Never hold a lock while performing network I/O, file I/O, or calling unknown external callbacks.
-   Do not invoke UI callbacks while holding TradingEngine or Account locks.

**Trading state ownership:**

-   TradingEngine is the coordinator for order execution.
-   Account mutations caused by orders must be coordinated through TradingEngine.
-   A fill must update order status, cash, positions, and trade history as one logical operation.
-   Duplicate or out-of-order quote events must not cause duplicate fills.
-   Agent 1 must document the chosen serialization strategy (synchronized critical sections or a dedicated single-thread executor) and test concurrent order/quote scenarios.

### 6.5 Financial and Input Validation

-   Quantity must be a positive integer.
-   Price and monetary inputs must be finite and greater than zero where required.
-   Reject `NaN`, positive/negative infinity, zero, and negative prices.
-   Validate symbol presence and normalize symbols consistently.
-   Prevent negative cash balances and negative position quantities unless a separately approved short-selling feature is added.
-   Guard divisions by zero and empty collections.
-   Avoid silently converting invalid values to zero.
-   Preserve the architecture's current `double` API unless a precision migration is approved.
-   Use a consistent tolerance for floating-point tests; the existing baseline is `0.001`, adjusted only when the tested magnitude and calculation justify it.

### 6.6 Error Handling and Logging

-   Never use empty catch blocks.
-   Catch the narrowest meaningful exception type.
-   Log useful context without secrets, credentials, or excessive market-tick noise.
-   Do not log API keys, authorization headers, or full credential-bearing URLs.
-   Distinguish validation failures, provider/network failures, parsing failures, and file I/O failures.
-   Do not hide failures behind a success status or a default value.
-   User-facing errors must be actionable and must not expose stack traces or secrets.
-   Preserve exception causes when wrapping exceptions.

### 6.7 Dependencies

Use the approved dependencies in the repository's `build.gradle`.

Approved baseline:

-   `org.java-websocket:Java-WebSocket:1.5.6`
-   `com.fasterxml.jackson.core:jackson-databind:2.17.0`
-   OpenJFX 21 modules used by the application.
-   JUnit 5.
-   Mockito 5.x.

Use JDK `HttpClient` for REST calls, `java.time` for date/time, and JDK I/O for CSV. Do not add a dependency for a capability already adequately provided by the JDK without approval from Agent 5.

### 6.8 API Key and Configuration Security

-   Never hardcode real API keys.
-   Never commit real credentials or credential-bearing configuration files.
-   Provide a safe example configuration containing placeholders only.
-   Ensure secret-bearing local files are covered by `.gitignore`.
-   Mask API keys in JavaFX settings controls.
-   Never print credentials in logs, exceptions, screenshots, test reports, or completion summaries.
-   Prefer environment variables or OS-backed credential storage for local secrets.
-   Do not place secrets in source-controlled test fixtures.
-   Validate that provider URLs and logs do not expose secrets unnecessarily.

### 6.9 Resource Lifecycle

-   Every WebSocket, executor, scheduled task, and file resource must have a defined owner and shutdown path.
-   `stop()` must be safe to call repeatedly where practical.
-   Application shutdown must stop the active provider and terminate owned executors.
-   Avoid daemon-thread-only cleanup as the sole resource-management strategy.
-   Provider switching must not leave orphan connections, duplicate listeners, or duplicate scheduled tasks.

------------------------------------------------------------------------

## 7. Provider-Specific Requirements

### 7.1 MockMarketService

-   Runs without network access or API keys.
-   Uses a `ScheduledExecutorService`.
-   Emits deterministic data when a seeded/test generator is injected; production random behavior may remain stochastic.
-   Emits positive finite prices only.
-   Supports subscribe/unsubscribe and clean shutdown.
-   Does not emit updates for symbols that are no longer subscribed.
-   Tests must not depend on real-time sleeps where deterministic scheduler control can be used.

### 7.2 Twelve Data and Finnhub

-   Parse provider messages into the common `Quote` contract.
-   Handle malformed, incomplete, and unexpected messages without crashing the application.
-   Handle connection close, timeout, and reconnect events.
-   Respect provider subscription formats and documented API constraints.
-   Do not assume every provider exposes identical fields or semantics.
-   Keep parser tests based on local JSON fixtures.

### 7.3 Alpha Vantage Scope Gate

The architecture document lists Alpha Vantage and describes REST polling / WebSocket, but the original implementation file allocation does not define a complete adapter. Before treating it as a release requirement:

1.  Agent 2 documents the intended Alpha Vantage API mode and supported quote fields.
2.  Agent 5 verifies that the proposed mode fits the project scope and available API constraints.
3.  The architecture document and provider contract are updated.
4.  Agent 2 receives explicit ownership of the adapter and parser/polling tests.

Until this gate is approved, do not present Alpha Vantage as a fully implemented provider merely because its enum value or settings field exists.

------------------------------------------------------------------------

## 8. Testing Strategy and Quality Gates

### 8.1 Test Layers

  -----------------------------------------------------------------------
  Layer                   Purpose                 Network
  ----------------------- ----------------------- -----------------------
  Unit                    Verify class behavior   Not allowed
                          and calculations        

  Module integration      Verify collaborating    Not allowed
                          classes within a module 

  Cross-module            Verify Engine, Market,  Mock/stub only
  integration             Analytics contracts     

  UI integration          Verify                  Mock/stub only
                          controller/service      
                          wiring and FX-thread    
                          behavior                

  E2E                     Verify full user        Mock provider by
                          workflow                default

  Live provider smoke     Optional manual         Allowed only with
  test                    verification of         explicit credentials
                          external APIs           and opt-in
  -----------------------------------------------------------------------

### 8.2 Required Test Coverage

**Agent 1 --- Core & Engine**

-   Order polymorphism.
-   Weighted average cost basis.
-   Partial sale and realized P&L.
-   Insufficient cash and insufficient shares.
-   Pending-order transitions and cancellation.
-   Duplicate quote and duplicate fill prevention.
-   Concurrent order placement and quote processing.
-   No negative cash or positions.

**Agent 2 --- Market**

-   Mock price generation and subscription lifecycle.
-   Twelve Data parser.
-   Finnhub parser.
-   Provider switching and subscription replay.
-   Listener registration/removal without duplicates.
-   Failed provider startup and fallback/rollback behavior.
-   Reconnection and clean shutdown.
-   Malformed provider messages.

**Agent 3 --- Analytics**

-   Win rate with wins, losses, zero-P&L trades, and no SELL trades.
-   Total realized P&L.
-   Maximum drawdown with empty, single-point, flat, rising, and falling histories.
-   Best/worst trade with ties and no eligible trades.
-   CSV escaping, BOM, UTF-8, timestamps, and I/O failure.

**Agent 4 --- UI**

-   Order-dialog validation.
-   MARKET/LIMIT field enablement.
-   Provider status rendering.
-   UI updates dispatched on the FX thread.
-   Controller behavior when services report errors.
-   Clean application shutdown.

**Agent 5 --- Cross-module/E2E**

-   Start with Mock provider.
-   Receive quotes.
-   Submit a Market BUY.
-   Verify cash and position updates.
-   Submit a Limit BUY and trigger execution with a controlled quote.
-   Submit a SELL and verify realized P&L.
-   Open Analytics and verify metrics.
-   Export CSV and verify output.
-   Switch providers using mocked adapters.
-   Simulate provider failure and verify the visible error/fallback state.
-   Close the application and verify resource shutdown.

### 8.3 Offline-First Requirement

`./gradlew test` MUST pass in a network-isolated environment.

-   Do not open live WebSocket connections in automated tests.
-   Do not require real API keys.
-   Use MockMarketService, Mockito, local JSON fixtures, and injected fake transports.
-   Tests must be deterministic and must clean up executors, temporary files, and listeners.
-   Avoid timing-sensitive assertions; use latches, controlled executors, or deterministic test doubles.

### 8.4 Required Local Verification

Before reporting a task as complete, run the relevant commands:

``` bash
./gradlew checkstyleMain checkstyleTest
./gradlew compileJava compileTestJava
./gradlew test
```

For a documentation-only change, code verification is not required; report that no code tests were run because no code changed.

For a module task, run the relevant tests during development and run the full required verification before integration when the repository is available.

Never claim a command passed if it was not executed.

------------------------------------------------------------------------

## 9. Git and Integration Workflow

### 9.1 Branching

Use one branch per implementation workstream, for example:

``` text
main
├── feature/core-engine
├── feature/market-data
├── feature/analytics
├── feature/javafx-ui
└── integration/qa
```

-   Do not commit unfinished work directly to `main`.
-   Do not force-push shared branches.
-   Do not rebase or reset another agent's branch without coordination.
-   Keep commits focused and descriptive.
-   Do not include generated build output, local secrets, IDE state, or unrelated files.

### 9.2 Integration Sequence

1.  Agree on shared contracts and compile-time skeletons.
2.  Implement independent modules in parallel.
3.  Integrate domain and market event flow.
4.  Integrate analytics with stable trade/equity history.
5.  Integrate UI with published service contracts.
6.  Run cross-module integration tests.
7.  Run full quality gates.
8.  Merge only after Agent 5 verifies the change.

### 9.3 Merge Requirements

A change is eligible for integration only when:

-   It stays within the owner's assigned scope.
-   Public API changes are documented and approved.
-   Relevant tests pass.
-   Checkstyle passes.
-   No secrets or unrelated generated files are included.
-   Known limitations and follow-up work are documented.

If integration reveals a defect in an owned module, Agent 5 should assign the correction to that module's owner.

------------------------------------------------------------------------

## 10. Agent Communication Protocol

### 10.1 Before Starting a Task

Each agent must:

1.  Read this document and the relevant architecture sections.
2.  Inspect existing code and tests before editing.
3.  Confirm file ownership.
4.  Identify upstream contracts and downstream consumers.
5.  Report any missing contract or blocking ambiguity.
6.  State a concise implementation plan for multi-file or cross-module tasks.

### 10.2 During Implementation

Agents must:

-   Keep changes scoped to the assigned task.
-   Preserve existing behavior unless a change is required and approved.
-   Notify affected agents before changing shared contracts.
-   Avoid editing another agent's owned files.
-   Record assumptions and known limitations.
-   Run relevant tests after meaningful changes.

### 10.3 Standard Completion Report

Every completed task must be reported using this format:

``` text
TASK:
AGENT:
STATUS: Complete / Partial / Blocked

IMPLEMENTED:
- Files created:
- Files modified:
- Features completed:

CONTRACTS:
- Public API changes:
- Downstream agents affected:
- Approval reference (if applicable):

VERIFICATION:
- Commands executed:
- Results:
- Tests added/updated:

KNOWN ISSUES:
- Remaining defects:
- Assumptions:
- Deferred work:

INTEGRATION NOTES:
- Required actions from other agents:
```

Do not report `Complete` when required implementation or verification is missing. Use `Partial` or `Blocked` and explain why.

### 10.4 Blocker Escalation

Escalate to Agent 5 when:

-   A required interface is missing or contradictory.
-   A task requires editing another agent's files.
-   An architectural decision is needed.
-   Build or tests fail due to cross-module incompatibility.
-   Provider/API behavior is uncertain.
-   A security or data-integrity issue is discovered.

A blocker report must include observed facts, reproduction steps, impact, and a proposed next action.

------------------------------------------------------------------------

## 11. Definition of Done

A task is complete only when all applicable conditions are satisfied:

1.  **Scope:** Changes are within the assigned ownership boundary.
2.  **Implementation:** Acceptance criteria are met.
3.  **Compilation:** `./gradlew compileJava compileTestJava` succeeds for code changes.
4.  **Checkstyle:** `./gradlew checkstyleMain checkstyleTest` reports zero warnings.
5.  **Tests:** Relevant tests pass; full test suite passes before integration.
6.  **Offline isolation:** Automated tests require no live network or API key.
7.  **Concurrency:** No known FX-thread violations, duplicate fills, unsafe shared-state mutations, or leaked resources.
8.  **Contracts:** Public API changes are documented and approved.
9.  **Security:** No credentials or sensitive local configuration are committed or logged.
10. **Reporting:** Standard completion report is provided with truthful verification results.

A feature is not considered fully integrated merely because its source code compiles in isolation.

------------------------------------------------------------------------

## 12. RACI Responsibility Matrix

  ----------------------------------------------------------------------------------------
  Package / Artifact           Agent 1     Agent 2     Agent 3     Agent 4 UI  Agent 5
                               Core        Market      Analytics               Lead
  ---------------------------- ----------- ----------- ----------- ----------- -----------
  `com.trading.model/**`       R/A         C           C           C           I

  `com.trading.engine/**`      R/A         C           I           C           I

  `com.trading.market/**`      C           R/A         I           C           I

  `com.trading.analytics/**`   C           I           R/A         C           I

  `com.trading.ui/**`          C           C           C           R/A         I

  `resources/fxml/**`          I           C           C           R/A         I

  `resources/css/**`           I           I           I           R/A         I

  `build.gradle` / Wrapper     C           C           C           C           R/A

  `config/checkstyle/**`       C           C           C           C           R/A

  Cross-module integration     C           C           C           C           R/A
  ----------------------------------------------------------------------------------------

**Legend:** R = Responsible (implements); A = Accountable (approves/owns outcome); C = Consulted; I = Informed.

------------------------------------------------------------------------

## 13. Development Phases

### Phase 1 --- Contract and Skeleton

-   Agent 1: Domain entities and engine interface skeleton.
-   Agent 2: Market interfaces, provider enum, Mock skeleton.
-   Agent 3: Analytics and exporter skeleton.
-   Agent 4: FXML/CSS skeleton and controller contracts.
-   Agent 5: Verify package boundaries, build, dependencies, and shared contracts.

### Phase 2 --- Independent Module Implementation

-   Agent 1: Order execution, account mutation, and tests.
-   Agent 2: Provider adapters, parsing, switching, and tests.
-   Agent 3: Analytics algorithms, CSV export, and tests.
-   Agent 4: Controllers, order dialog, settings, and UI tests.

### Phase 3 --- Cross-Module Integration

-   Integrate Market quote events with TradingEngine.
-   Integrate Account/Trade/Equity history with Analytics.
-   Integrate provider manager, TradingEngine, Analytics, and controllers with UI.
-   Resolve contract mismatches through Agent 5.

### Phase 4 --- QA and Release Readiness

-   Run offline unit and integration tests.
-   Run Checkstyle and compilation.
-   Execute the full Mock-based E2E workflow.
-   Verify provider failure handling and shutdown.
-   Review security, documentation, and known limitations.

------------------------------------------------------------------------

## 14. Open Decisions Register

The following issues are visible in the original architecture/coordination documents and must not be silently guessed.

  ---------------------------------------------------------------------------------------
  ID                Decision required              Proposed owner    Required outcome
  ----------------- ------------------------------ ----------------- --------------------
  ADR-001           Confirm Alpha Vantage release  Agent 5 + Agent 2 Approved provider
                    scope and REST polling                           scope and tests
                    contract                                         

  ADR-002           Define pending BUY cash and    Agent 5 + Agent 1 No
                    pending SELL share reservation                   overspend/oversell
                                                                     policy

  ADR-003           Define                         Agent 5 + Agent 1 Stable
                    `Account.getEquityHistory()`                     equity-history
                    sampling and valuation policy                    semantics

  ADR-004           Confirm simplified last-price  Agent 5 + Agent   Explicit
                    execution or Bid/Ask execution 1 + Agent 2       execution-price rule

  ADR-005           Confirm provider switch        Agent 5 + Agent   State model and UI
                    failure behavior and fallback  2 + Agent 4       behavior
                    policy                                           

  ADR-006           Add best/worst trade methods   Agent 5 + Agent 3 Synchronized
                    to architecture specification                    analytics contract

  ADR-007           Confirm monetary precision     Agent 5 + Agent   Consistent numeric
                    strategy (`double` baseline or 1 + Agent 3       policy
                    BigDecimal migration)                            
  ---------------------------------------------------------------------------------------

Until an item is approved, agents may implement unrelated work but must not invent a final behavior for the affected decision.

------------------------------------------------------------------------

## 15. Final Directive

The goal is not merely to produce code quickly. The goal is to produce a coherent, testable, maintainable application through parallel development without interface drift, ownership conflicts, hidden assumptions, or unverified completion claims.

When in doubt:

1.  Preserve the approved architecture.
2.  Preserve module ownership.
3.  Ask Agent 5 to resolve cross-agent issues.
4.  Record decisions before changing shared contracts.
5.  Prefer deterministic tests and explicit failure handling.
6.  Never claim work or verification that has not actually been completed.
