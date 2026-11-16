package com.trading.sim.ui;

import com.trading.analytics.AnalyticsService;
import com.trading.analytics.CsvExporter;
import com.trading.model.Trade;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

/**
 * Shows performance metrics, the equity curve and the CSV export for one account snapshot.
 *
 * <p>This controller contains presentation logic only. Every number comes from
 * {@link AnalyticsService} and the file is written by {@link CsvExporter}, so the financial
 * rules live in one place.
 */
public final class AnalyticsController {

  private static final Logger LOGGER = System.getLogger(AnalyticsController.class.getName());

  private static final String PROFIT_STYLE_CLASS = "profit-text";
  private static final String LOSS_STYLE_CLASS = "loss-text";
  private static final String PERCENT_FORMAT = "%.2f%%";
  private static final String MONEY_FORMAT = "$%,.2f";
  private static final String NEGATIVE_MONEY_FORMAT = "-$%,.2f";
  private static final String DEFAULT_FILE_NAME = "trading_report.csv";
  private static final String CSV_FILTER_LABEL = "CSV files (*.csv)";
  private static final String CSV_FILTER_PATTERN = "*.csv";
  private static final int MIN_POINTS_FOR_CURVE = 2;

  @FXML
  private Label tradeCountLabel;

  @FXML
  private Label winRateLabel;

  @FXML
  private Label realizedPnLLabel;

  @FXML
  private Label maxDrawdownLabel;

  @FXML
  private LineChart<Number, Number> equityChart;

  @FXML
  private Label emptyStateLabel;

  @FXML
  private Button btnExportCsv;

  /** Immutable snapshot of the trades currently shown; replaced as a whole by loadData. */
  private List<Trade> trades = List.of();

  /** Called by the FXML loader: shows the empty state until {@link #loadData} is called. */
  @FXML
  private void initialize() {
    render(List.of(), List.of());
  }

  /**
   * Renders the report for the given account snapshot. Must be called on the JavaFX
   * Application Thread.
   *
   * @param tradeHistory the executed trades; null is treated as empty
   * @param equityHistory the equity values in chronological order; null is treated as empty
   */
  public void loadData(List<Trade> tradeHistory, List<Double> equityHistory) {
    render(snapshotOf(tradeHistory), snapshotOf(equityHistory));
  }

  private void render(List<Trade> tradeSnapshot, List<Double> equitySnapshot) {
    this.trades = tradeSnapshot;

    tradeCountLabel.setText(Integer.toString(tradeSnapshot.size()));
    winRateLabel.setText(percent(AnalyticsService.calculateWinRate(tradeSnapshot)));
    maxDrawdownLabel.setText(percent(AnalyticsService.calculateMaxDrawdown(equitySnapshot)));
    showNetPnL(AnalyticsService.calculateTotalPnL(tradeSnapshot));
    showEquityCurve(equitySnapshot);
    btnExportCsv.setDisable(tradeSnapshot.isEmpty());
  }

  private void showNetPnL(double netPnL) {
    realizedPnLLabel.setText(money(netPnL));
    realizedPnLLabel.getStyleClass().removeAll(PROFIT_STYLE_CLASS, LOSS_STYLE_CLASS);
    realizedPnLLabel.getStyleClass().add(netPnL >= 0.0 ? PROFIT_STYLE_CLASS : LOSS_STYLE_CLASS);
  }

  private void showEquityCurve(List<Double> equitySnapshot) {
    boolean hasCurve = equitySnapshot.size() >= MIN_POINTS_FOR_CURVE;
    equityChart.setVisible(hasCurve);
    emptyStateLabel.setVisible(!hasCurve);

    XYChart.Series<Number, Number> series = new XYChart.Series<>();
    if (hasCurve) {
      for (int index = 0; index < equitySnapshot.size(); index++) {
        series.getData().add(new XYChart.Data<>(index, equitySnapshot.get(index)));
      }
    }
    equityChart.getData().setAll(List.of(series));
  }

  @FXML
  private void exportCsv() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export trade history");
    chooser.setInitialFileName(DEFAULT_FILE_NAME);
    chooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter(CSV_FILTER_LABEL, CSV_FILTER_PATTERN));
    File file = chooser.showSaveDialog(btnExportCsv.getScene().getWindow());
    if (file == null) {
      return; // The user cancelled the dialog.
    }

    Path target = file.toPath();
    List<Trade> snapshot = trades;
    Task<Void> exportTask = new Task<>() {
      @Override
      protected Void call() throws IOException {
        CsvExporter.exportTrades(snapshot, target);
        return null;
      }
    };
    // Task callbacks run on the JavaFX Application Thread, so they may touch controls.
    exportTask.setOnSucceeded(event -> {
      btnExportCsv.setDisable(false);
      showAlert(
          Alert.AlertType.INFORMATION,
          "Export complete",
          "Saved " + snapshot.size() + " trades to " + file.getName() + ".");
    });
    exportTask.setOnFailed(event -> {
      btnExportCsv.setDisable(false);
      LOGGER.log(Level.WARNING, "CSV export failed.", exportTask.getException());
      showAlert(
          Alert.AlertType.ERROR,
          "Export failed",
          "The report could not be saved. Check that the folder exists, that you may write"
              + " to it, and that the file is not open in another program.");
    });

    btnExportCsv.setDisable(true);
    // The file is written on a background thread so the window never freezes. shutdown()
    // lets the submitted task finish and then ends the thread, so nothing is leaked.
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(exportTask);
    executor.shutdown();
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type, message, ButtonType.OK);
    alert.initOwner(btnExportCsv.getScene().getWindow());
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.showAndWait();
  }

  private static <T> List<T> snapshotOf(List<T> source) {
    if (source == null) {
      return List.of();
    }
    return Collections.unmodifiableList(new ArrayList<>(source));
  }

  private static String percent(double value) {
    return String.format(Locale.US, PERCENT_FORMAT, value);
  }

  private static String money(double value) {
    String format = value < 0.0 ? NEGATIVE_MONEY_FORMAT : MONEY_FORMAT;
    return String.format(Locale.US, format, Math.abs(value));
  }
}
