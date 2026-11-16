package com.trading.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class representing a stock trading order.
 */
public abstract class Order {

  private final String orderId;
  private final String symbol;
  private final boolean isBuy;
  private final int quantity;
  private OrderStatus status;
  private final Instant createdAt;

  /**
   * Constructs a new order with an automatically generated ID and current timestamp.
   *
   * @param symbol the ticker symbol
   * @param isBuy true for a BUY order, false for a SELL order
   * @param quantity the number of shares to trade (must be positive)
   */
  protected Order(String symbol, boolean isBuy, int quantity) {
    this(UUID.randomUUID().toString(), symbol, isBuy, quantity, Instant.now());
  }

  /**
   * Constructs a new order with explicit metadata.
   *
   * @param orderId the unique order identifier
   * @param symbol the ticker symbol
   * @param isBuy true for a BUY order, false for a SELL order
   * @param quantity the number of shares to trade (must be positive)
   * @param createdAt the creation timestamp
   */
  protected Order(
      String orderId, String symbol, boolean isBuy, int quantity, Instant createdAt) {
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("Order ID must not be null or blank.");
    }
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Symbol must not be null or blank.");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero.");
    }
    this.orderId = orderId.trim();
    this.symbol = symbol.trim().toUpperCase(Locale.ROOT);
    this.isBuy = isBuy;
    this.quantity = quantity;
    this.status = OrderStatus.PENDING;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null.");
  }

  /**
   * Determines whether this order can be executed at the given market price.
   *
   * @param currentPrice the current market price
   * @return true if the order is eligible for execution, false otherwise
   */
  public abstract boolean canExecute(double currentPrice);

  /**
   * Validates that a price value is finite and strictly positive.
   *
   * @param price the price to validate
   * @return true if finite and greater than zero
   */
  protected static boolean isValidPrice(double price) {
    return Double.isFinite(price) && price > 0.0;
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

  public synchronized OrderStatus getStatus() {
    return status;
  }

  /**
   * Updates the status of this order while preventing invalid transitions from terminal states.
   *
   * @param newStatus the target status
   */
  public synchronized void setStatus(OrderStatus newStatus) {
    Objects.requireNonNull(newStatus, "OrderStatus must not be null.");
    if (this.status == newStatus) {
      return;
    }
    if (this.status != OrderStatus.PENDING) {
      throw new IllegalStateException(
          "Cannot transition order from terminal status " + this.status + " to " + newStatus);
    }
    this.status = newStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}