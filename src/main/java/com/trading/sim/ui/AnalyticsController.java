package com.trading.sim.ui;

import com.trading.model.Trade;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Displays lightweight performance metrics from the current account snapshot. */
public final class AnalyticsController {

  @FXML
  private Label tradeCountLabel;

  @FXML
  private Label winRateLabel;

  @FXML
  private Label realizedPnLLabel;

  @FXML
  private Label maxDrawdownLabel;

  /** Loads account data into the report. */
  public void initData(List<Trade> trades, List<Double> equityHistory) {
    long sellCount = trades.stream().filter(trade -> !trade.isBuy()).count();
    long winningTrades = trades.stream()
        .filter(trade -> !trade.isBuy() && trade.getRealizedPnL() > 0.0)
        .count();
    double realizedPnL = trades.stream().mapToDouble(Trade::getRealizedPnL).sum();
    double drawdown = calculateMaxDrawdown(equityHistory);
    tradeCountLabel.setText(Long.toString(trades.size()));
    winRateLabel.setText(sellCount == 0 ? "0.00%" : String.format(
        "%.2f%%", winningTrades * 100.0 / sellCount));
    realizedPnLLabel.setText(String.format("$%,.2f", realizedPnL));
    maxDrawdownLabel.setText(String.format("%.2f%%", drawdown));
  }

  private double calculateMaxDrawdown(List<Double> equityHistory) {
    double peak = 0.0;
    double maximum = 0.0;
    for (double equity : equityHistory) {
      if (equity > peak) {
        peak = equity;
      }
      if (peak > 0.0) {
        maximum = Math.max(maximum, (peak - equity) / peak * 100.0);
      }
    }
    return maximum;
  }
}
