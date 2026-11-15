package com.trading.model;

import java.time.Instant;
import java.util.Objects;

public class Quote {

  private final String symbol;
  private final double price;
  private final double changePercent;
  private final Instant timestamp;

  public Quote(String symbol, double price, double changePercent, Instant timestamp) {
    if (symbol == null || symbol.trim().isEmpty()) {
      throw new IllegalArgumentException("Symbol cannot be null or empty");
    }
    this.symbol = symbol.toUpperCase().trim();
    this.price = price;
    this.changePercent = changePercent;
    this.timestamp = timestamp != null ? timestamp : Instant.now();
  }

  public Quote(String symbol, double price, Instant timestamp) {
    this(symbol, price, 0.0, timestamp);
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
    if (!(obj instanceof Quote)) {
      return false;
    }
    Quote other = (Quote) obj;
    return Double.compare(other.price, price) == 0
        && Double.compare(other.changePercent, changePercent) == 0
        && Objects.equals(symbol, other.symbol)
        && Objects.equals(timestamp, other.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, price, changePercent, timestamp);
  }

  @Override
  public String toString() {
    return "Quote{"
        + "symbol='" + symbol + '\''
        + ", price=" + price
        + ", changePercent=" + changePercent
        + ", timestamp=" + timestamp
        + '}';
  }
}
