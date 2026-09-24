package com.trading.sim.ui;

import java.io.IOException;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Controls the local-data dashboard shown before service integration. */
public final class MainController {

  private final ObservableList<QuoteRow> quotes = FXCollections.observableArrayList(
      new QuoteRow("AAPL", 182.50, 1.20),
      new QuoteRow("MSFT", 415.20, -0.45),
      new QuoteRow("TSLA", 175.80, 3.10));

  @FXML
  private Label chartTitleLabel;

  @FXML
  private LineChart<Number, Number> priceChart;

  @FXML
  private TableView<QuoteRow> watchlistTable;

  @FXML
  private TableView<SimpleRow> positionsTable;

  @FXML
  private TableView<SimpleRow> pendingOrdersTable;

  @FXML
  private TableView<SimpleRow> tradeHistoryTable;

  @FXML
  private void initialize() {
    configureWatchlist();
    configureSummaryTable(positionsTable, List.of(
        "AAPL   50 shares   Avg $176.20   Market $182.50   +$315.00",
        "TSLA   20 shares   Avg $168.40   Market $175.80   +$148.00"));
    configureSummaryTable(pendingOrdersTable, List.of(
        "BUY MSFT   10 shares   LIMIT $408.00   Pending"));
    configureSummaryTable(tradeHistoryTable, List.of(
        "BUY AAPL   50 shares   $176.20   Filled",
        "SELL NVDA  10 shares   $121.40   Filled"));
    watchlistTable.getSelectionModel().selectFirst();
  }

  @FXML
  private void openOrderDialog() {
    try {
      FXMLLoader loader = new FXMLLoader(
          MainController.class.getResource("/fxml/OrderDialog.fxml"));
      Parent root = loader.load();
      Scene scene = new Scene(root);
      scene.getStylesheets().add(
          MainController.class.getResource("/css/dark-theme.css").toExternalForm());

      Stage dialog = new Stage();
      dialog.initOwner(watchlistTable.getScene().getWindow());
      dialog.initModality(Modality.WINDOW_MODAL);
      dialog.setResizable(false);
      dialog.setTitle("Place Order");
      dialog.setScene(scene);
      dialog.showAndWait();
    } catch (IOException exception) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Unable to open order entry");
      alert.setHeaderText("The order dialog could not be loaded.");
      alert.setContentText(exception.getMessage());
      alert.showAndWait();
    }
  }

  private void configureWatchlist() {
    TableColumn<QuoteRow, String> symbolColumn = new TableColumn<>("Symbol");
    symbolColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().symbol()));
    TableColumn<QuoteRow, String> priceColumn = new TableColumn<>("Last Price");
    priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(
        String.format("$%,.2f", cell.getValue().price())));
    TableColumn<QuoteRow, String> changeColumn = new TableColumn<>("Change");
    changeColumn.setCellValueFactory(cell -> new SimpleStringProperty(
        String.format("%+.2f%%", cell.getValue().changePercent())));
    watchlistTable.getColumns().setAll(List.of(symbolColumn, priceColumn, changeColumn));
    watchlistTable.setItems(quotes);
    watchlistTable.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue != null) {
            drawPriceSeries(newValue);
          }
        });
  }

  private void configureSummaryTable(TableView<SimpleRow> table, List<String> entries) {
    TableColumn<SimpleRow, String> descriptionColumn = new TableColumn<>("Details");
    descriptionColumn.setCellValueFactory(
        cell -> new SimpleStringProperty(cell.getValue().description()));
    descriptionColumn.prefWidthProperty().bind(table.widthProperty().subtract(4));
    table.getColumns().setAll(List.of(descriptionColumn));
    table.setItems(FXCollections.observableArrayList(
        entries.stream().map(SimpleRow::new).toList()));
  }

  private void drawPriceSeries(QuoteRow quote) {
    XYChart.Series<Number, Number> series = new XYChart.Series<>();
    series.setName(quote.symbol());
    for (int minute = 1; minute <= 30; minute++) {
      double movement = Math.sin(minute * 0.65) * quote.price() * 0.007;
      series.getData().add(new XYChart.Data<>(minute, quote.price() + movement));
    }
    chartTitleLabel.setText(quote.symbol() + " intraday price");
    priceChart.getData().setAll(List.of(series));
  }

  private record QuoteRow(String symbol, double price, double changePercent) {
  }

  private record SimpleRow(String description) {
  }
}
