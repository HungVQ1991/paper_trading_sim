package com.trading.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable value object representing a real-time market quote for a symbol.
 */
public final class Quote {

  private final String symbol;
  private final double price;
  private final double changePercent;
  private final Instant timestamp;

  /**
   * Creates a quote with current timestamp and zero percent change.
   *
   * @param symbol the ticker symbol
   * @param price the latest market price
   */
  public Quote(String symbol, double price) {
    this(symbol, price, 0.0, Instant.now());
  }

  /**
   * Creates a quote with current timestamp.
   *
   * @param symbol the ticker symbol
   * @param price the latest market price
   * @param changePercent the percentage price change
   */
  public Quote(String symbol, double price, double changePercent) {
    this(symbol, price, changePercent, Instant.now());
  }

  /**
   * Creates a complete market quote.
   *
   * @param symbol the ticker symbol
   * @param price the latest market price (must be finite and positive)
   * @param changePercent the percentage change (must be finite)
   * @param timestamp the quote observation timestamp
   */
  public Quote(String symbol, double price, double changePercent, Instant timestamp) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Symbol must not be null or blank.");
    }
    if (!Double.isFinite(price) || price <= 0.0) {
      throw new IllegalArgumentException("Price must be finite and greater than zero.");
    }
    if (!Double.isFinite(changePercent)) {
      throw new IllegalArgumentException("Change percent must be finite.");
    }
    this.symbol = symbol.trim().toUpperCase(Locale.ROOT);
    this.price = price;
    this.changePercent = changePercent;
    this.timestamp = Objects.requireNonNull(timestamp, "Timestamp must not be null.");
  }

  public String getSymbol() {
    return symbol;
  }

  public double getPrice() {
    return price;
  }

  public double getChangePercent() {
    return changePercent;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Quote other)) {
      return false;
    }
    return Double.compare(price, other.price) == 0
        && Double.compare(changePercent, other.changePercent) == 0
        && symbol.equals(other.symbol)
        && timestamp.equals(other.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, price, changePercent, timestamp);
  }

  @Override
  public String toString() {
    return "Quote{symbol='"
        + symbol
        + "', price="
        + price
        + ", changePercent="
        + changePercent
        + ", timestamp="
        + timestamp
        + '}';
  }
}