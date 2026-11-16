package com.trading.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trading.model.Account;
import com.trading.model.LimitOrder;
import com.trading.model.MarketOrder;
import com.trading.model.Order;
import com.trading.model.OrderStatus;
import com.trading.model.Position;
import com.trading.model.Quote;
import com.trading.model.Trade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TradingEngine} order validation, pending order execution, cancellation,
 * and thread safety.
 */
class TradingEngineTest {

  private static final double DELTA = 0.001;

  private Account account;
  private TradingEngine engine;

  @BeforeEach
  void setUp() {
    account = new Account(1_000.0);
    engine = new TradingEngine(account);
  }

  @Test
  @DisplayName("Reject Market BUY when total order cost exceeds available cash")
  void rejectMarketBuyWithInsufficientCash() {
    Order order = new MarketOrder("AAPL", true, 100);

    boolean placed = engine.placeOrder(order, 15.0);

    assertFalse(placed);
    assertEquals(OrderStatus.REJECTED, order.getStatus());
    assertEquals(1_000.0, account.getCashBalance(), DELTA);
    assertTrue(account.getTradeHistory().isEmpty());
  }

  @Test
  @DisplayName("Limit BUY stays PENDING until quote reaches limit price, then fills")
  void limitBuyLifecycleFromPendingToFilled() {
    LimitOrder limitBuy = new LimitOrder("AAPL", true, 10, 90.0);

    boolean placed = engine.placeOrder(limitBuy, 100.0);
    assertTrue(placed);
    assertEquals(OrderStatus.PENDING, limitBuy.getStatus());
    assertEquals(1, engine.getPendingOrders().size());
    assertEquals(1_000.0, account.getCashBalance(), DELTA);

    engine.onPriceUpdate(new Quote("AAPL", 95.0));
    assertEquals(OrderStatus.PENDING, limitBuy.getStatus());
    assertEquals(1, engine.getPendingOrders().size());

    engine.onPriceUpdate(new Quote("AAPL", 89.0));
    assertEquals(OrderStatus.FILLED, limitBuy.getStatus());
    assertTrue(engine.getPendingOrders().isEmpty());
    assertEquals(110.0, account.getCashBalance(), DELTA);

    Position aaplPosition = account.getPositions().get("AAPL");
    assertEquals(10, aaplPosition.getQuantity());
    assertEquals(89.0, aaplPosition.getAveragePrice(), DELTA);
    assertEquals(1, account.getTradeHistory().size());
  }

  @Test
  @DisplayName("Market BUY followed by Market SELL updates cash, position, and realized PnL")
  void marketBuyAndSellRealizedPnL() {
    Order buyOrder = new MarketOrder("MSFT", true, 5);
    assertTrue(engine.placeOrder(buyOrder, 100.0));
    assertEquals(500.0, account.getCashBalance(), DELTA);

    Order sellOrder = new MarketOrder("MSFT", false, 3);
    assertTrue(engine.placeOrder(sellOrder, 120.0));
    assertEquals(860.0, account.getCashBalance(), DELTA);
    assertEquals(2, account.getPositions().get("MSFT").getQuantity());

    List<Trade> trades = account.getTradeHistory();
    assertEquals(2, trades.size());
    assertEquals(60.0, trades.get(1).getRealizedPnL(), DELTA);
    assertEquals(1100.0, account.getTotalValue(Map.of("MSFT", 120.0)), DELTA);
  }

  @Test
  @DisplayName("Reject SELL order when shares are insufficient and prevent overselling via pending")
  void rejectInsufficientSharesAndPendingOversell() {
    Order buyOrder = new MarketOrder("TSLA", true, 5);
    assertTrue(engine.placeOrder(buyOrder, 100.0));

    Order pendingSell = new LimitOrder("TSLA", false, 4, 120.0);
    assertTrue(engine.placeOrder(pendingSell, 100.0));

    Order excessSell = new LimitOrder("TSLA", false, 2, 125.0);
    assertFalse(engine.placeOrder(excessSell, 100.0));
    assertEquals(OrderStatus.REJECTED, excessSell.getStatus());
  }

  @Test
  @DisplayName("Cancelled pending order is removed and never fills on subsequent price updates")
  void cancelPendingOrderPreventsExecution() {
    LimitOrder limitBuy = new LimitOrder("AAPL", true, 5, 100.0);
    assertTrue(engine.placeOrder(limitBuy, 110.0));

    assertTrue(engine.cancelOrder(limitBuy.getOrderId()));
    assertEquals(OrderStatus.CANCELLED, limitBuy.getStatus());
    assertTrue(engine.getPendingOrders().isEmpty());

    engine.onPriceUpdate(new Quote("AAPL", 95.0));
    assertEquals(OrderStatus.CANCELLED, limitBuy.getStatus());
    assertEquals(1_000.0, account.getCashBalance(), DELTA);
    assertTrue(account.getTradeHistory().isEmpty());
  }

  @Test
  @DisplayName("Concurrent order placement and quote updates preserve cash and share invariants")
  void concurrentOrdersAndQuotesMaintainInvariants() throws Exception {
    Account concurrentAccount = new Account(10_000.0);
    TradingEngine concurrentEngine = new TradingEngine(concurrentAccount);

    ExecutorService executor = Executors.newFixedThreadPool(8);
    CountDownLatch startLatch = new CountDownLatch(1);
    List<Callable<Void>> tasks = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
      tasks.add(
          () -> {
            startLatch.await();
            Order order = new LimitOrder("AAPL", true, 1, 100.0);
            concurrentEngine.placeOrder(order, 105.0);
            return null;
          });
      tasks.add(
          () -> {
            startLatch.await();
            concurrentEngine.onPriceUpdate(new Quote("AAPL", 99.0));
            return null;
          });
    }

    List<Future<Void>> futures = new ArrayList<>();
    for (Callable<Void> task : tasks) {
      futures.add(executor.submit(task));
    }
    startLatch.countDown();
    for (Future<Void> future : futures) {
      future.get(5, TimeUnit.SECONDS);
    }
    concurrentEngine.onPriceUpdate(new Quote("AAPL", 99.0));

    executor.shutdownNow();

    assertTrue(concurrentAccount.getCashBalance() >= 0.0);
    assertEquals(20, concurrentAccount.getPositionQuantity("AAPL"));
    assertEquals(20, concurrentAccount.getTradeHistory().size());
    assertEquals(10_000.0 - (20 * 99.0), concurrentAccount.getCashBalance(), DELTA);
  }
}