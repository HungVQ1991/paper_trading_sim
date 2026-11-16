package com.trading.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the user's cash balance, stock positions, trade history, and equity snapshots.
 */
public class Account {

  public static final double DEFAULT_INITIAL_CASH = 100_000.0;

  private double cashBalance;
  private final Map<String, Position> positions;
  private final List<Trade> tradeHistory;
  private final List<Double> equityHistory;

  /**
   * Creates an account with the default initial cash balance ($100,000.00).
   */
  public Account() {
    this(DEFAULT_INITIAL_CASH);
  }

  /**
   * Creates an account with the specified initial cash balance.
   *
   * @param initialCash the starting cash balance (must be finite and non-negative)
   */
  public Account(double initialCash) {
    if (!Double.isFinite(initialCash) || initialCash < 0.0) {
      throw new IllegalArgumentException("Initial cash must be finite and non-negative.");
    }
    this.cashBalance = initialCash;
    this.positions = new ConcurrentHashMap<>();
    this.tradeHistory = new ArrayList<>();
    this.equityHistory = new ArrayList<>();
    this.equityHistory.add(initialCash);
  }

  /**
   * Deducts the specified amount from the cash balance if sufficient funds exist.
   *
   * @param amount the positive amount to deduct
   * @return true if the deduction succeeded, false if insufficient funds
   */
  public synchronized boolean deductCash(double amount) {
    if (!Double.isFinite(amount) || amount <= 0.0) {
      throw new IllegalArgumentException("Deduction amount must be finite and greater than zero.");
    }
    if (amount > this.cashBalance) {
      return false;
    }
    this.cashBalance -= amount;
    return true;
  }

  /**
   * Adds the specified amount to the cash balance.
   *
   * @param amount the positive amount to add
   */
  public synchronized void addCash(double amount) {
    if (!Double.isFinite(amount) || amount <= 0.0) {
      throw new IllegalArgumentException("Deposit amount must be finite and greater than zero.");
    }
    this.cashBalance += amount;
  }

  /**
   * Retrieves the existing position for a symbol or creates a new empty position.
   *
   * @param symbol the ticker symbol
   * @return the managed position instance
   */
  public synchronized Position getOrCreatePosition(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("Symbol must not be null or blank.");
    }
    String normalized = symbol.trim().toUpperCase(Locale.ROOT);
    return positions.computeIfAbsent(normalized, Position::new);
  }

  /**
   * Returns the current available share count for a symbol.
   *
   * @param symbol the ticker symbol
   * @return the held quantity, or 0 if no position exists
   */
  public synchronized int getPositionQuantity(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      return 0;
    }
    Position position = positions.get(symbol.trim().toUpperCase(Locale.ROOT));
    return position == null ? 0 : position.getQuantity();
  }

  /**
   * Calculates total account value (cash plus market value of all held positions).
   *
   * @param currentPrices map of ticker symbols to their current market prices
   * @return the total portfolio equity
   */
  public synchronized double getTotalValue(Map<String, Double> currentPrices) {
    Objects.requireNonNull(currentPrices, "currentPrices must not be null.");
    double totalMarketValue = 0.0;
    for (Map.Entry<String, Position> entry : positions.entrySet()) {
      Position position = entry.getValue();
      int qty = position.getQuantity();
      if (qty <= 0) {
        continue;
      }
      Double marketPrice = currentPrices.get(entry.getKey());
      double valuationPrice =
          (marketPrice != null && Double.isFinite(marketPrice) && marketPrice > 0.0)
              ? marketPrice
              : position.getAveragePrice();
      totalMarketValue += position.getMarketValue(valuationPrice);
    }
    return this.cashBalance + totalMarketValue;
  }

  /**
   * Records an executed trade in the account's history.
   *
   * @param trade the executed trade
   */
  public synchronized void recordTrade(Trade trade) {
    Objects.requireNonNull(trade, "Trade must not be null.");
    this.tradeHistory.add(trade);
  }

  /**
   * Records a portfolio equity valuation point in the account's equity history.
   *
   * @param equity the total portfolio equity (must be finite and non-negative)
   */
  public synchronized void recordEquitySnapshot(double equity) {
    if (!Double.isFinite(equity) || equity < 0.0) {
      throw new IllegalArgumentException("Equity snapshot must be finite and non-negative.");
    }
    this.equityHistory.add(equity);
  }

  /**
   * Resets the account to the specified starting cash balance and clears positions and history.
   *
   * @param initialCash the new starting cash balance
   */
  public synchronized void reset(double initialCash) {
    if (!Double.isFinite(initialCash) || initialCash < 0.0) {
      throw new IllegalArgumentException("Initial cash must be finite and non-negative.");
    }
    this.cashBalance = initialCash;
    this.positions.clear();
    this.tradeHistory.clear();
    this.equityHistory.clear();
    this.equityHistory.add(initialCash);
  }

  public synchronized double getCashBalance() {
    return cashBalance;
  }

  /**
   * Returns an unmodifiable snapshot of current positions with defensive copies.
   *
   * @return unmodifiable map of symbol to position snapshot
   */
  public synchronized Map<String, Position> getPositions() {
    Map<String, Position> snapshot = new HashMap<>();
    for (Map.Entry<String, Position> entry : positions.entrySet()) {
      snapshot.put(entry.getKey(), entry.getValue().copy());
    }
    return Collections.unmodifiableMap(snapshot);
  }

  /**
   * Returns an unmodifiable snapshot of executed trades.
   *
   * @return unmodifiable list of trades
   */
  public synchronized List<Trade> getTradeHistory() {
    return List.copyOf(tradeHistory);
  }

  /**
   * Returns an unmodifiable snapshot of recorded portfolio equity values.
   *
   * @return unmodifiable list of equity values
   */
  public synchronized List<Double> getEquityHistory() {
    return List.copyOf(equityHistory);
  }
}