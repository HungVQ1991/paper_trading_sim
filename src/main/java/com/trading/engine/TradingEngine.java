package com.trading.engine;

import com.trading.model.Account;
import com.trading.model.LimitOrder;
import com.trading.model.MarketOrder;
import com.trading.model.Order;
import com.trading.model.OrderStatus;
import com.trading.model.Position;
import com.trading.model.Quote;
import com.trading.model.Trade;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates order validation, immediate market execution, pending limit order lifecycle,
 * and quote-triggered fills.
 *
 * <p>Thread-safety policy: all order mutations, cancellations, and price-update fills are
 * serialized on this engine instance's monitor.
 */
public class TradingEngine {

  private final Account account;
  private final List<Order> pendingOrders = new CopyOnWriteArrayList<>();
  private final Map<String, Double> latestPrices = new ConcurrentHashMap<>();

  /**
   * Creates a trading engine bound to the given user account.
   *
   * @param account the trading account
   */
  public TradingEngine(Account account) {
    this.account = Objects.requireNonNull(account, "Account must not be null.");
  }

  /**
   * Validates and places a market or limit order.
   *
   * @param order the order to place
   * @param currentPrice the latest market price for the order's symbol
   * @return true if the order was filled (MarketOrder) or queued (LimitOrder), false if rejected
   */
  public synchronized boolean placeOrder(Order order, double currentPrice) {
    Objects.requireNonNull(order, "Order must not be null.");
    if (order.getStatus() != OrderStatus.PENDING || containsPendingOrder(order.getOrderId())) {
      return false;
    }
    if (!Double.isFinite(currentPrice) || currentPrice <= 0.0) {
      order.setStatus(OrderStatus.REJECTED);
      return false;
    }

    latestPrices.put(order.getSymbol(), currentPrice);

    if (order.isBuy()) {
      double unitPrice =
          (order instanceof LimitOrder limitOrder) ? limitOrder.getLimitPrice() : currentPrice;
      double requiredCash = order.getQuantity() * unitPrice;
      double availableCash = account.getCashBalance() - getReservedBuyCash();
      if (availableCash < requiredCash) {
        order.setStatus(OrderStatus.REJECTED);
        return false;
      }
    } else {
      int heldShares = account.getPositionQuantity(order.getSymbol());
      int availableShares = heldShares - getReservedSellShares(order.getSymbol());
      if (availableShares < order.getQuantity()) {
        order.setStatus(OrderStatus.REJECTED);
        return false;
      }
    }

    if (order instanceof MarketOrder) {
      return executeOrder(order, currentPrice);
    }
    if (order instanceof LimitOrder) {
      order.setStatus(OrderStatus.PENDING);
      pendingOrders.add(order);
      return true;
    }

    order.setStatus(OrderStatus.REJECTED);
    return false;
  }

  /**
   * Processes a real-time quote update and executes any matching pending orders.
   *
   * @param quote the incoming market quote
   */
  public synchronized void onPriceUpdate(Quote quote) {
    if (quote == null) {
      return;
    }
    double price = quote.getPrice();
    if (!Double.isFinite(price) || price <= 0.0) {
      return;
    }

    latestPrices.put(quote.getSymbol(), price);

    for (Order order : pendingOrders) {
      if (order.getStatus() == OrderStatus.PENDING
          && order.getSymbol().equals(quote.getSymbol())
          && order.canExecute(price)) {
        pendingOrders.remove(order);
        executeOrder(order, price);
      }
    }
  }

  /**
   * Cancels a pending order by its unique identifier.
   *
   * @param orderId the ID of the pending order to cancel
   * @return true if the order was found and cancelled, false otherwise
   */
  public synchronized boolean cancelOrder(String orderId) {
    if (orderId == null || orderId.isBlank()) {
      return false;
    }
    for (Order order : pendingOrders) {
      if (order.getOrderId().equals(orderId) && order.getStatus() == OrderStatus.PENDING) {
        pendingOrders.remove(order);
        order.setStatus(OrderStatus.CANCELLED);
        return true;
      }
    }
    return false;
  }

  /**
   * Returns an unmodifiable snapshot of currently pending orders.
   *
   * @return unmodifiable list of pending orders
   */
  public synchronized List<Order> getPendingOrders() {
    return List.copyOf(pendingOrders);
  }

  public Account getAccount() {
    return account;
  }

  private boolean executeOrder(Order order, double execPrice) {
    if (order.getStatus() != OrderStatus.PENDING) {
      return false;
    }
    double realizedPnL = 0.0;
    if (order.isBuy()) {
      double totalCost = order.getQuantity() * execPrice;
      if (!account.deductCash(totalCost)) {
        order.setStatus(OrderStatus.REJECTED);
        return false;
      }
      Position position = account.getOrCreatePosition(order.getSymbol());
      position.buyMore(order.getQuantity(), execPrice);
    } else {
      Position position = account.getOrCreatePosition(order.getSymbol());
      if (position.getQuantity() < order.getQuantity()) {
        order.setStatus(OrderStatus.REJECTED);
        return false;
      }
      realizedPnL = position.sell(order.getQuantity(), execPrice);
      account.addCash(order.getQuantity() * execPrice);
    }

    order.setStatus(OrderStatus.FILLED);
    latestPrices.put(order.getSymbol(), execPrice);

    Trade trade =
        new Trade(
            UUID.randomUUID().toString(),
            order.getOrderId(),
            order.getSymbol(),
            order.isBuy(),
            order.getQuantity(),
            execPrice,
            realizedPnL,
            Instant.now());
    account.recordTrade(trade);
    account.recordEquitySnapshot(account.getTotalValue(latestPrices));
    return true;
  }

  private boolean containsPendingOrder(String orderId) {
    for (Order existing : pendingOrders) {
      if (existing.getOrderId().equals(orderId)) {
        return true;
      }
    }
    return false;
  }

  private double getReservedBuyCash() {
    double reserved = 0.0;
    for (Order pending : pendingOrders) {
      if (pending.getStatus() == OrderStatus.PENDING
          && pending.isBuy()
          && pending instanceof LimitOrder limitOrder) {
        reserved += pending.getQuantity() * limitOrder.getLimitPrice();
      }
    }
    return reserved;
  }

  private int getReservedSellShares(String symbol) {
    int reserved = 0;
    for (Order pending : pendingOrders) {
      if (pending.getStatus() == OrderStatus.PENDING
          && !pending.isBuy()
          && pending.getSymbol().equals(symbol)) {
        reserved += pending.getQuantity();
      }
    }
    return reserved;
  }
}