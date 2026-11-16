package com.trading.analytics;

import com.trading.model.Trade;
import java.time.Instant;

/**
 * Builds deterministic {@link Trade} objects for the analytics tests.
 *
 * <p>Every fixture uses a fixed timestamp and an explicit trade ID, so no test depends on the
 * system clock or on random UUIDs.
 */
final class TradeFixtures {

  static final Instant BASE_TIME = Instant.parse("2026-03-24T10:15:30Z");
  static final String DEFAULT_SYMBOL = "AAPL";
  static final int DEFAULT_QUANTITY = 10;
  static final double DEFAULT_PRICE = 182.50;

  private static final double NO_REALIZED_PNL = 0.0;

  private TradeFixtures() {
    // Utility class: prevents instantiation.
  }

  /** Creates a BUY trade with default symbol, quantity and price. BUYs realize no P&amp;L. */
  static Trade buy(String tradeId) {
    return trade(
        tradeId, DEFAULT_SYMBOL, true, DEFAULT_QUANTITY, DEFAULT_PRICE, NO_REALIZED_PNL,
        BASE_TIME);
  }

  /** Creates a SELL trade with the given realized P&amp;L and default other fields. */
  static Trade sell(String tradeId, double realizedPnL) {
    return trade(
        tradeId, DEFAULT_SYMBOL, false, DEFAULT_QUANTITY, DEFAULT_PRICE, realizedPnL, BASE_TIME);
  }

  /** Creates a trade with every field chosen by the caller. */
  static Trade trade(
      String tradeId,
      String symbol,
      boolean isBuy,
      int quantity,
      double executedPrice,
      double realizedPnL,
      Instant timestamp) {
    return new Trade(tradeId, symbol, isBuy, quantity, executedPrice, realizedPnL, timestamp);
  }
}
