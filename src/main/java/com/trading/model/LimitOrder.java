package com.trading.model;

import java.time.Instant;

/**
 * Represents a limit order that executes only when the market price reaches the limit threshold.
 */
public class LimitOrder extends Order {

  private final double limitPrice;

  /**
   * Creates a new limit order.
   *
   * @param symbol the ticker symbol
   * @param isBuy true for BUY, false for SELL
   * @param quantity the positive share quantity
   * @param limitPrice the target limit price (must be finite and greater than zero)
   */
  public LimitOrder(String symbol, boolean isBuy, int quantity, double limitPrice) {
    super(symbol, isBuy, quantity);
    if (!isValidPrice(limitPrice)) {
      throw new IllegalArgumentException("Limit price must be finite and greater than zero.");
    }
    this.limitPrice = limitPrice;
  }

  /**
   * Creates a new limit order with explicit identifier and creation time.
   *
   * @param orderId the unique order ID
   * @param symbol the ticker symbol
   * @param isBuy true for BUY, false for SELL
   * @param quantity the positive share quantity
   * @param limitPrice the target limit price
   * @param createdAt the creation timestamp
   */
  public LimitOrder(
      String orderId,
      String symbol,
      boolean isBuy,
      int quantity,
      double limitPrice,
      Instant createdAt) {
    super(orderId, symbol, isBuy, quantity, createdAt);
    if (!isValidPrice(limitPrice)) {
      throw new IllegalArgumentException("Limit price must be finite and greater than zero.");
    }
    this.limitPrice = limitPrice;
  }

  public double getLimitPrice() {
    return limitPrice;
  }

  @Override
  public boolean canExecute(double currentPrice) {
    if (!isValidPrice(currentPrice)) {
      return false;
    }
    if (isBuy()) {
      return currentPrice <= limitPrice;
    }
    return currentPrice >= limitPrice;
  }
}