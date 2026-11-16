package com.trading.model;

import java.time.Instant;

/**
 * Represents a market order that executes immediately at the current market price.
 */
public class MarketOrder extends Order {

  /**
   * Creates a new market order.
   *
   * @param symbol the ticker symbol
   * @param isBuy true for BUY, false for SELL
   * @param quantity the positive share quantity
   */
  public MarketOrder(String symbol, boolean isBuy, int quantity) {
    super(symbol, isBuy, quantity);
  }

  /**
   * Creates a new market order with explicit identifier and creation time.
   *
   * @param orderId the unique order ID
   * @param symbol the ticker symbol
   * @param isBuy true for BUY, false for SELL
   * @param quantity the positive share quantity
   * @param createdAt the creation timestamp
   */
  public MarketOrder(
      String orderId, String symbol, boolean isBuy, int quantity, Instant createdAt) {
    super(orderId, symbol, isBuy, quantity, createdAt);
  }

  @Override
  public boolean canExecute(double currentPrice) {
    return isValidPrice(currentPrice);
  }
}