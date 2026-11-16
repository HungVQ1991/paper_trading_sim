package com.trading.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record of an executed trade.
 */
public final class Trade {

  private final String tradeId;
  private final String orderId;
  private final String symbol;
  private final boolean isBuy;
  private final int quantity;
  private final double executedPrice;
  private final double realizedPnL;
  private final Instant timestamp;

  /**
   * Creates a trade with a generated trade ID and current timestamp.
   *
   * @param orderId the originating order ID
   * @param symbol the ticker symbol
   * @param isBuy true for BUY, false for SELL
   * @param quantity the executed share quantity
   * @param executedPrice the execution price per share
   * @param realizedPnL the realized profit or loss
   */
  public Trade(
      String orderId,
      String symbol,
      boolean isBuy,
      int quantity,
      double executedPrice,
      double realizedPnL) {
    this(
        UUID.randomUUID().toString(),
        orderId,
        symbol,
        isBuy,
        quantity,
        executedPrice,
        realizedPnL,
        Instant.now());
  }

  /**
   * Creates a trade with an explicit trade ID and timestamp, using tradeId as orderId fallback.
   *
   * @param tradeId the unique trade ID
   * @param symbol the ticker symbol
   * @param isBuy true for BUY, false for SELL
   * @param quantity the executed share quantity
   * @param executedPrice the execution price per share
   * @param realizedPnL the realized profit or loss
   * @param timestamp the execution timestamp
   */
  public Trade(
      String tradeId,
      String symbol,
      boolean isBuy,
      int quantity,
      double executedPrice,
      double realizedPnL,
      Instant timestamp) {
    this(tradeId, tradeId, symbol, isBuy, quantity, executedPrice, realizedPnL, timestamp);
  }

  /**
   * Creates a complete immutable trade record.
   *
   * @param tradeId the unique trade ID
   * @param orderId the originating order ID
   * @param symbol the ticker symbol
   * @param isBuy true for BUY, false for SELL
   * @param quantity the executed share quantity (must be positive)
   * @param executedPrice the execution price per share (must be finite and positive)
   * @param realizedPnL the realized profit or loss (must be finite)
   * @param timestamp the execution timestamp
   */
  public Trade(
      String tradeId,
      String orderId,
      String symbol,
      boolean isBuy,
      int quantity,
      double executedPrice,
      double realizedPnL,
      Instant timestamp) {
    if (tradeId == null || tradeId.isBlank()) {
      throw new IllegalArgumentException("Trade ID must not be null or blank.");
    }
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("Order ID must not be null or blank.");
    }
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Symbol must not be null or blank.");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero.");
    }
    if (!Double.isFinite(executedPrice) || executedPrice <= 0.0) {
      throw new IllegalArgumentException("Executed price must be finite and greater than zero.");
    }
    if (!Double.isFinite(realizedPnL)) {
      throw new IllegalArgumentException("Realized PnL must be finite.");
    }
    this.tradeId = tradeId.trim();
    this.orderId = orderId.trim();
    this.symbol = symbol.trim().toUpperCase(Locale.ROOT);
    this.isBuy = isBuy;
    this.quantity = quantity;
    this.executedPrice = executedPrice;
    this.realizedPnL = realizedPnL;
    this.timestamp = Objects.requireNonNull(timestamp, "Timestamp must not be null.");
  }

  public String getTradeId() {
    return tradeId;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getSymbol() {
    return symbol;
  }

  public boolean isBuy() {
    return isBuy;
  }

  public int getQuantity() {
    return quantity;
  }

  public double getExecutedPrice() {
    return executedPrice;
  }

  public double getRealizedPnL() {
    return realizedPnL;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Trade other)) {
      return false;
    }
    return isBuy == other.isBuy
        && quantity == other.quantity
        && Double.compare(executedPrice, other.executedPrice) == 0
        && Double.compare(realizedPnL, other.realizedPnL) == 0
        && tradeId.equals(other.tradeId)
        && orderId.equals(other.orderId)
        && symbol.equals(other.symbol)
        && timestamp.equals(other.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        tradeId, orderId, symbol, isBuy, quantity, executedPrice, realizedPnL, timestamp);
  }

  @Override
  public String toString() {
    return "Trade{tradeId='"
        + tradeId
        + "', orderId='"
        + orderId
        + "', symbol='"
        + symbol
        + "', isBuy="
        + isBuy
        + ", quantity="
        + quantity
        + ", executedPrice="
        + executedPrice
        + ", realizedPnL="
        + realizedPnL
        + ", timestamp="
        + timestamp
        + '}';
  }
}