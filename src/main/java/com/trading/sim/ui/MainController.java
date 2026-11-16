package com.trading.sim.ui;

import com.trading.engine.TradingEngine;
import com.trading.model.Account;
import com.trading.model.LimitOrder;
import com.trading.model.Order;
import com.trading.model.Position;
import com.trading.model.Quote;
import com.trading.model.Trade;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Coordinates the trading dashboard and its offline mock market feed. */
public final class MainController {

  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

  private final Account account = new Account(100_000.0);
  private final TradingEngine tradingEngine = new TradingEngine(account);
  private final Map<String, Quote> latestQuotes = new HashMap<>();
  private final Map<String, List<Quote>> priceHistories = new HashMap<>();
  private final ObservableList<Quote> watchlist = FXCollections.observableArrayList();
  private final ObservableList<PositionRow> positionRows = FXCollections.observableArrayList();
  private final ObservableList<Order> pendingOrders = FXCollections.observableArrayList();
  private final ObservableList<Trade> trades = FXCollections.observableArrayList();
  private final ScheduledExecutorService marketFeed = Executors.newSingleThreadScheduledExecutor();

  @FXML
  private Label cashBalanceLabel;

  @FXML
  private Label totalEquityLabel;

  @FXML
  private Label totalPnLLabel;

  @FXML
  private Label providerStatusLabel;

  @FXML
  private ComboBox<String> providerSelector;

  @FXML
  private TableView<Quote> watchlistTable;

  @FXML
  private TableView<PositionRow> positionsTable;

  @FXML
  private TableView<Order> pendingOrdersTable;

  @FXML
  private TableView<Trade> tradeHistoryTable;

  @FXML
  private LineChart<String, Number> priceChart;

  @FXML
  private CategoryAxis timeAxis;

  @FXML
  private NumberAxis priceAxis;

  @FXML
  private Label chartTitleLabel;

  @FXML
  private Slider chartRangeSlider;

  @FXML
  private Label chartRangeLabel;

  @FXML
  private void initialize() {
    configureProviderSelector();
    configureWatchlist();
    configurePositions();
    configurePendingOrders();
    configureTradeHistory();
    seedQuotes();
    configureChartRange();
    Quote selected = watchlistTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      renderChartHistory(selected.getSymbol());
    }
    refreshDashboard();
    startMockFeed();
  }

  @FXML
  private void openOrderDialog() {
    try {
      FXMLLoader loader = new FXMLLoader(
          MainController.class.getResource("/fxml/OrderDialog.fxml"));
      Parent root = loader.load();
      OrderDialogController controller = loader.getController();
      controller.configure(tradingEngine, this::getLatestPrice, this::refreshDashboard);

      Stage dialog = new Stage();
      dialog.initOwner(watchlistTable.getScene().getWindow());
      dialog.initModality(Modality.WINDOW_MODAL);
      dialog.setResizable(false);
      dialog.setTitle("Place Order");
      dialog.setScene(new Scene(root));
      dialog.getScene().getStylesheets().add(
          MainController.class.getResource("/css/dark-theme.css").toExternalForm());
      dialog.showAndWait();
    } catch (IOException exception) {
      showError("Unable to open order entry", exception.getMessage());
    }
  }

  @FXML
  private void openAnalytics() {
    try {
      FXMLLoader loader = new FXMLLoader(
          MainController.class.getResource("/fxml/AnalyticsView.fxml"));
      Parent root = loader.load();
      AnalyticsController controller = loader.getController();
      controller.initData(account.getTradeHistory(), account.getEquityHistory());
      showChildWindow("Performance Analytics", root, 600, 480);
    } catch (IOException exception) {
      showError("Unable to open analytics", exception.getMessage());
    }
  }

  @FXML
  private void openSettings() {
    try {
      FXMLLoader loader = new FXMLLoader(
          MainController.class.getResource("/fxml/SettingsView.fxml"));
      Parent root = loader.load();
      SettingsController controller = loader.getController();
      controller.configure(providerSelector.getValue(), selectedProvider -> {
        providerSelector.setValue(selectedProvider);
        providerStatusLabel.setText("● " + selectedProvider + " provider connected");
      });
      showChildWindow("Provider Settings", root, 460, 300);
    } catch (IOException exception) {
      showError("Unable to open settings", exception.getMessage());
    }
  }

  @FXML
  private void exportTrades() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export Trade History");
    chooser.setInitialFileName("trade-history.csv");
    chooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("CSV files", "*.csv"));
    java.io.File selectedFile = chooser.showSaveDialog(watchlistTable.getScene().getWindow());
    if (selectedFile == null) {
      return;
    }
    Path target = selectedFile.toPath();
    try {
      StringBuilder csv = new StringBuilder("\uFEFF");
      csv.append("TradeID,Timestamp,Symbol,Action,Quantity,ExecutedPrice,RealizedPnL\n");
      for (Trade trade : trades) {
        csv.append(csvValue(trade.getTradeId())).append(',')
            .append(csvValue(trade.getTimestamp().toString())).append(',')
            .append(csvValue(trade.getSymbol())).append(',')
            .append(trade.isBuy() ? "BUY" : "SELL").append(',')
            .append(trade.getQuantity()).append(',')
            .append(trade.getExecutedPrice()).append(',')
            .append(trade.getRealizedPnL()).append('\n');
      }
      Files.writeString(target, csv.toString(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      showError("Unable to export trades", exception.getMessage());
    }
  }

  @FXML
  private void exitApplication() {
    Stage stage = (Stage) watchlistTable.getScene().getWindow();
    stage.close();
  }

  public void shutdown() {
    marketFeed.shutdownNow();
  }

  public void onQuoteReceived(Quote quote) {
    if (quote == null) {
      return;
    }
    tradingEngine.onPriceUpdate(quote);
    Platform.runLater(() -> {
      latestQuotes.put(quote.getSymbol(), quote);
      replaceQuote(quote);
      appendPriceHistory(quote);
      renderChartHistory(quote.getSymbol());
      refreshDashboard();
    });
  }

  private void configureProviderSelector() {
    providerSelector.setItems(FXCollections.observableArrayList("Mock Demo"));
    providerSelector.getSelectionModel().selectFirst();
    providerSelector.setOnAction(event ->
        providerStatusLabel.setText("● " + providerSelector.getValue() + " provider connected"));
  }

  private void configureWatchlist() {
    TableColumn<Quote, String> symbol = new TableColumn<>("Symbol");
    symbol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSymbol()));
    TableColumn<Quote, String> price = new TableColumn<>("Last Price");
    price.setCellValueFactory(cell -> new SimpleStringProperty(
        currency(cell.getValue().getPrice())));
    TableColumn<Quote, String> change = new TableColumn<>("Change");
    change.setCellValueFactory(cell -> new SimpleStringProperty(
        String.format(Locale.US, "%+.2f%%", cell.getValue().getChangePercent())));
    change.setCellFactory(column -> new TableCell<Quote, String>() {
      @Override
      protected void updateItem(String value, boolean empty) {
        super.updateItem(value, empty);
        setText(value);
        getStyleClass().removeAll("profit-text", "loss-text");
        if (!empty && value != null) {
          getStyleClass().add(value.startsWith("-") ? "loss-text" : "profit-text");
        }
      }
    });
    watchlistTable.getColumns().setAll(symbol, price, change);
    watchlistTable.setItems(watchlist);
    watchlistTable.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, selected) -> {
          if (selected != null) {
            chartTitleLabel.setText(selected.getSymbol() + " intraday price");
            renderChartHistory(selected.getSymbol());
          }
        });
  }

  private void configurePositions() {
    TableColumn<PositionRow, String> symbol = column("Symbol", PositionRow::symbol);
    TableColumn<PositionRow, String> quantity = column("Quantity",
        row -> Integer.toString(row.quantity()));
    TableColumn<PositionRow, String> average = column("Avg Cost",
        row -> currency(row.averagePrice()));
    TableColumn<PositionRow, String> market = column("Market Value",
        row -> currency(row.marketValue()));
    TableColumn<PositionRow, String> pnl = column("Unrealized P&L",
        row -> currency(row.unrealizedPnL()));
    pnl.setCellFactory(column -> new TableCell<PositionRow, String>() {
      @Override
      protected void updateItem(String value, boolean empty) {
        super.updateItem(value, empty);
        setText(value);
        getStyleClass().removeAll("profit-text", "loss-text");
        if (!empty && value != null) {
          getStyleClass().add(value.startsWith("-") ? "loss-text" : "profit-text");
        }
      }
    });
    positionsTable.getColumns().setAll(symbol, quantity, average, market, pnl);
    positionsTable.setItems(positionRows);
  }

  private void configurePendingOrders() {
    TableColumn<Order, String> id = column("Order ID", Order::getOrderId);
    TableColumn<Order, String> symbol = column("Symbol", Order::getSymbol);
    TableColumn<Order, String> side = column("Side", order -> order.isBuy() ? "BUY" : "SELL");
    TableColumn<Order, String> type = column("Type",
        order -> order instanceof LimitOrder ? "LIMIT" : "MARKET");
    TableColumn<Order, String> quantity = column("Quantity",
        order -> Integer.toString(order.getQuantity()));
    TableColumn<Order, String> price = column("Limit Price", order ->
        order instanceof LimitOrder limit ? currency(limit.getLimitPrice()) : "—");
    TableColumn<Order, Void> action = new TableColumn<>("Action");
    action.setCellFactory(column -> new TableCell<Order, Void>() {
      private final javafx.scene.control.Button cancel = new javafx.scene.control.Button("Cancel");

      {
        cancel.getStyleClass().add("sell-button");
        cancel.setOnAction(event -> {
          Order order = getTableView().getItems().get(getIndex());
          if (tradingEngine.cancelOrder(order.getOrderId())) {
            refreshDashboard();
          }
        });
      }

      @Override
      protected void updateItem(Void value, boolean empty) {
        super.updateItem(value, empty);
        setGraphic(empty ? null : cancel);
      }
    });
    pendingOrdersTable.getColumns().setAll(id, symbol, side, type, quantity, price, action);
    pendingOrdersTable.setItems(pendingOrders);
  }

  private void configureTradeHistory() {
    TableColumn<Trade, String> time = column("Time",
        trade -> TIME_FORMAT.format(trade.getTimestamp()));
    TableColumn<Trade, String> symbol = column("Symbol", Trade::getSymbol);
    TableColumn<Trade, String> side = column("Action", trade -> trade.isBuy() ? "BUY" : "SELL");
    TableColumn<Trade, String> quantity = column("Quantity",
        trade -> Integer.toString(trade.getQuantity()));
    TableColumn<Trade, String> price = column("Executed Price",
        trade -> currency(trade.getExecutedPrice()));
    TableColumn<Trade, String> pnl = column("Realized P&L",
        trade -> currency(trade.getRealizedPnL()));
    tradeHistoryTable.getColumns().setAll(time, symbol, side, quantity, price, pnl);
    tradeHistoryTable.setItems(trades);
  }

  private void seedQuotes() {
    addQuote(new Quote("AAPL", 182.50, 1.20));
    addQuote(new Quote("MSFT", 415.20, -0.45));
    addQuote(new Quote("TSLA", 175.80, 3.10));
    watchlistTable.getSelectionModel().selectFirst();
  }

  private void startMockFeed() {
    marketFeed.scheduleAtFixedRate(() -> {
      List<Quote> quotes = new ArrayList<>(latestQuotes.values());
      for (Quote current : quotes) {
        double time = System.nanoTime() / 1_000_000_000.0;
        double movement = current.getPrice()
            * (Math.sin(time * 1.7 + current.getSymbol().hashCode()) * 0.006
                + Math.cos(time * 0.8) * 0.003);
        double price = Math.max(1.0, current.getPrice() + movement);
        double change = current.getChangePercent() + movement / current.getPrice() * 100;
        onQuoteReceived(new Quote(current.getSymbol(), price, change));
      }
    }, 1, 1, TimeUnit.SECONDS);
  }

  private void refreshDashboard() {
    Map<String, Double> prices = new HashMap<>();
    latestQuotes.forEach((symbol, quote) -> prices.put(symbol, quote.getPrice()));
    double equity = account.getTotalValue(prices);
    double pnl = equity - Account.DEFAULT_INITIAL_CASH;
    cashBalanceLabel.setText(currency(account.getCashBalance()));
    totalEquityLabel.setText(currency(equity));
    totalPnLLabel.setText(String.format(Locale.US, "%s (%+.2f%%)",
        currency(pnl), pnl / Account.DEFAULT_INITIAL_CASH * 100));
    totalPnLLabel.getStyleClass().removeAll("profit-text", "loss-text");
    totalPnLLabel.getStyleClass().add(pnl < 0 ? "loss-text" : "profit-text");

    positionRows.clear();
    account.getPositions().forEach((symbol, position) -> {
      if (position.getQuantity() > 0) {
        double price = getLatestPrice(symbol);
        positionRows.add(new PositionRow(
            symbol,
            position.getQuantity(),
            position.getAveragePrice(),
            position.getMarketValue(price),
            position.getUnrealizedPnL(price)));
      }
    });
    pendingOrders.setAll(tradingEngine.getPendingOrders());
    trades.setAll(account.getTradeHistory());
  }

  private void addQuote(Quote quote) {
    latestQuotes.put(quote.getSymbol(), quote);
    priceHistories.computeIfAbsent(quote.getSymbol(), symbol -> new ArrayList<>()).add(quote);
    watchlist.add(quote);
  }

  private void appendPriceHistory(Quote quote) {
    List<Quote> history = priceHistories.computeIfAbsent(
        quote.getSymbol(), symbol -> new ArrayList<>());
    history.add(quote);
    while (history.size() > ChartRange.YEAR.points()) {
      history.remove(0);
    }
  }

  private void replaceQuote(Quote quote) {
    for (int index = 0; index < watchlist.size(); index++) {
      if (watchlist.get(index).getSymbol().equals(quote.getSymbol())) {
        watchlist.set(index, quote);
        return;
      }
    }
    watchlist.add(quote);
  }

  private void configureChartRange() {
    chartRangeSlider.setMin(0);
    chartRangeSlider.setMax(ChartRange.values().length - 1);
    chartRangeSlider.setMajorTickUnit(1);
    chartRangeSlider.setBlockIncrement(1);
    chartRangeSlider.setSnapToTicks(true);
    chartRangeSlider.setShowTickMarks(false);
    chartRangeSlider.setValue(0);
    chartRangeSlider.valueProperty().addListener(
        (observable, oldValue, newValue) -> {
          ChartRange range = selectedChartRange();
          chartRangeLabel.setText(range.label());
          Quote selected = watchlistTable.getSelectionModel().getSelectedItem();
          if (selected != null) {
            renderChartHistory(selected.getSymbol());
          }
        });
    chartRangeLabel.setText(selectedChartRange().label());
  }

  private void renderChartHistory(String symbol) {
    Quote selected = watchlistTable.getSelectionModel().getSelectedItem();
    if (selected == null || !selected.getSymbol().equals(symbol)) {
      return;
    }
    ChartRange range = selectedChartRange();
    List<Quote> history = priceHistories.getOrDefault(symbol, List.of());
    if (history.isEmpty()) {
      return;
    }
    int start = Math.max(0, history.size() - range.points());
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    for (Quote quote : history.subList(start, history.size())) {
      series.getData().add(new XYChart.Data<>(
          formatChartTime(quote.getTimestamp()), quote.getPrice()));
    }
    priceChart.getData().setAll(series);
  }

  private ChartRange selectedChartRange() {
    int index = (int) Math.round(chartRangeSlider.getValue());
    return ChartRange.values()[Math.max(0, Math.min(index, ChartRange.values().length - 1))];
  }

  private String formatChartTime(Instant timestamp) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    return formatter.format(timestamp);
  }

  private double getLatestPrice(String symbol) {
    Quote quote = latestQuotes.get(symbol);
    return quote == null ? 0.0 : quote.getPrice();
  }

  private void showChildWindow(String title, Parent root, int width, int height) {
    Stage child = new Stage();
    child.initOwner(watchlistTable.getScene().getWindow());
    child.initModality(Modality.WINDOW_MODAL);
    child.setTitle(title);
    child.setScene(new Scene(root, width, height));
    child.getScene().getStylesheets().add(
        MainController.class.getResource("/css/dark-theme.css").toExternalForm());
    child.showAndWait();
  }

  private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.showAndWait();
  }

  private static String currency(double value) {
    return CURRENCY.format(value);
  }

  private static String csvValue(String value) {
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private enum ChartRange {
    DAY("24 hours", 24),
    WEEK("1 week", 28),
    MONTH("1 month", 30),
    YEAR("1 year", 52);

    private final String label;
    private final int points;

    ChartRange(String label, int points) {
      this.label = label;
      this.points = points;
    }

    private String label() {
      return label;
    }

    private int points() {
      return points;
    }
  }

  private static <T> TableColumn<T, String> column(
      String title, java.util.function.Function<T, String> valueFactory) {
    TableColumn<T, String> column = new TableColumn<>(title);
    column.setCellValueFactory(cell -> new SimpleStringProperty(valueFactory.apply(cell.getValue())));
    return column;
  }

  /** Immutable view row for the positions table. */
  public record PositionRow(
      String symbol, int quantity, double averagePrice, double marketValue, double unrealizedPnL) {
  }
}
