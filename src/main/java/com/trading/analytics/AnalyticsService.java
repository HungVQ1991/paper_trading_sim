package com.trading.analytics;

import com.trading.model.Trade;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stateless performance statistics computed from trade history and equity history.
 *
 * <p>This is a utility class: every method is static, it holds no state, and it cannot be
 * instantiated. It depends only on the domain model, never on JavaFX or market providers.
 *
 * <p>Shared rules for every method:
 * <ul>
 *   <li>A null list is treated like an empty list.</li>
 *   <li>Input lists are copied before use and are never modified.</li>
 *   <li>Only SELL trades are "closed" trades. A BUY opens a position and realizes nothing, so
 *       BUY records are ignored.</li>
 *   <li>A null element (or a non-finite equity value) is a caller bug and is rejected with an
 *       {@link IllegalArgumentException} instead of being silently counted as zero.</li>
 * </ul>
 */
public final class AnalyticsService {

  private static final double PERCENT = 100.0;
  private static final int MIN_POINTS_FOR_DRAWDOWN = 2;

  private AnalyticsService() {
    // Utility class: prevents instantiation.
  }

  /**
   * Calculates the percentage of SELL trades that closed with a profit.
   *
   * <p>Formula: {@code winning SELLs / all SELLs * 100}. A SELL wins only when its realized
   * P&amp;L is strictly greater than zero, so a break-even SELL counts as a non-win.
   *
   * @param trades the executed trades; may be null or empty
   * @return the win rate in percent (0.0 to 100.0), or 0.0 when there are no SELL trades
   * @throws IllegalArgumentException if the list contains a null element
   */
  public static double calculateWinRate(List<Trade> trades) {
    List<Trade> sells = sellTrades(trades);
    if (sells.isEmpty()) {
      return 0.0;
    }
    int wins = 0;
    for (Trade sell : sells) {
      if (sell.getRealizedPnL() > 0.0) {
        wins++;
      }
    }
    return wins * PERCENT / sells.size();
  }

  /**
   * Calculates the total realized profit or loss over all SELL trades.
   *
   * @param trades the executed trades; may be null or empty
   * @return the sum of realized P&amp;L, or 0.0 when there are no SELL trades
   * @throws IllegalArgumentException if the list contains a null element
   */
  public static double calculateTotalPnL(List<Trade> trades) {
    double total = 0.0;
    for (Trade sell : sellTrades(trades)) {
      total += sell.getRealizedPnL();
    }
    return total;
  }

  /**
   * Calculates the largest peak-to-trough decline of the equity series.
   *
   * <p>The algorithm walks the series once and remembers the highest value seen so far (the
   * running peak). At each point the drawdown is {@code (peak - equity) / peak * 100}; the
   * result is the largest drawdown found. While the peak is zero or negative the percentage
   * is undefined, so those points are skipped rather than divided by.
   *
   * @param equityHistory the account equity values in chronological order; may be null
   * @return the maximum drawdown in percent (0.0 or greater); 0.0 for fewer than two points
   *     or when the series never reaches a positive peak
   * @throws IllegalArgumentException if the list contains a null, NaN or infinite value
   */
  public static double calculateMaxDrawdown(List<Double> equityHistory) {
    if (equityHistory == null) {
      return 0.0;
    }
    List<Double> points = new ArrayList<>(equityHistory);
    for (Double point : points) {
      if (point == null || !Double.isFinite(point)) {
        throw new IllegalArgumentException("Equity history must contain only finite values.");
      }
    }
    if (points.size() < MIN_POINTS_FOR_DRAWDOWN) {
      return 0.0;
    }
    double peak = points.get(0);
    double maxDrawdown = 0.0;
    for (double equity : points) {
      if (equity > peak) {
        peak = equity;
      }
      if (peak > 0.0) {
        double drawdown = (peak - equity) / peak * PERCENT;
        if (drawdown > maxDrawdown) {
          maxDrawdown = drawdown;
        }
      }
    }
    return maxDrawdown;
  }

  /**
   * Finds the SELL trade with the highest realized P&amp;L.
   *
   * <p>When several trades share the highest value, the earliest one in list order is
   * returned.
   *
   * @param trades the executed trades; may be null or empty
   * @return the best SELL trade, or an empty Optional when there are no SELL trades
   * @throws IllegalArgumentException if the list contains a null element
   */
  public static Optional<Trade> getBestTrade(List<Trade> trades) {
    Trade best = null;
    for (Trade sell : sellTrades(trades)) {
      // Strict ">" keeps the earliest trade when two are equal.
      if (best == null || sell.getRealizedPnL() > best.getRealizedPnL()) {
        best = sell;
      }
    }
    return Optional.ofNullable(best);
  }

  /**
   * Finds the SELL trade with the lowest realized P&amp;L (the largest loss).
   *
   * <p>When several trades share the lowest value, the earliest one in list order is
   * returned.
   *
   * @param trades the executed trades; may be null or empty
   * @return the worst SELL trade, or an empty Optional when there are no SELL trades
   * @throws IllegalArgumentException if the list contains a null element
   */
  public static Optional<Trade> getWorstTrade(List<Trade> trades) {
    Trade worst = null;
    for (Trade sell : sellTrades(trades)) {
      // Strict "<" keeps the earliest trade when two are equal.
      if (worst == null || sell.getRealizedPnL() < worst.getRealizedPnL()) {
        worst = sell;
      }
    }
    return Optional.ofNullable(worst);
  }

  /**
   * Returns the SELL trades of the given list, in their original order.
   *
   * <p>The input is copied first, so a caller that changes its list on another thread cannot
   * affect a calculation that is already running.
   */
  private static List<Trade> sellTrades(List<Trade> trades) {
    List<Trade> sells = new ArrayList<>();
    if (trades == null) {
      return sells;
    }
    for (Trade trade : new ArrayList<>(trades)) {
      if (trade == null) {
        throw new IllegalArgumentException("Trade list must not contain null elements.");
      }
      if (!trade.isBuy()) {
        sells.add(trade);
      }
    }
    return sells;
  }
}
