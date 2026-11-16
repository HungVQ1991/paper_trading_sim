package com.trading.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying polymorphic execution rules across the {@link Order} hierarchy.
 */
class OrderPolymorphismTest {

  @Test
  @DisplayName("MarketOrder always executes at valid positive market prices")
  void marketOrderCanExecuteAtValidPrices() {
    Order marketBuy = new MarketOrder("AAPL", true, 10);
    Order marketSell = new MarketOrder("AAPL", false, 5);

    assertTrue(marketBuy.canExecute(100.0));
    assertTrue(marketBuy.canExecute(200.0));
    assertTrue(marketSell.canExecute(0.01));

    assertFalse(marketBuy.canExecute(0.0));
    assertFalse(marketBuy.canExecute(-10.0));
    assertFalse(marketBuy.canExecute(Double.NaN));
    assertFalse(marketBuy.canExecute(Double.POSITIVE_INFINITY));
  }

  @Test
  @DisplayName("LimitOrder BUY executes only when currentPrice <= limitPrice")
  void limitBuyOrderExecutesAtOrBelowLimitPrice() {
    Order limitBuy = new LimitOrder("AAPL", true, 10, 150.0);

    assertFalse(limitBuy.canExecute(155.0));
    assertTrue(limitBuy.canExecute(150.0));
    assertTrue(limitBuy.canExecute(145.0));
    assertFalse(limitBuy.canExecute(0.0));
    assertFalse(limitBuy.canExecute(Double.NaN));
  }

  @Test
  @DisplayName("LimitOrder SELL executes only when currentPrice >= limitPrice")
  void limitSellOrderExecutesAtOrAboveLimitPrice() {
    Order limitSell = new LimitOrder("TSLA", false, 5, 200.0);

    assertFalse(limitSell.canExecute(195.0));
    assertTrue(limitSell.canExecute(200.0));
    assertTrue(limitSell.canExecute(205.0));
    assertFalse(limitSell.canExecute(-205.0));
    assertFalse(limitSell.canExecute(Double.POSITIVE_INFINITY));
  }

  @Test
  @DisplayName("Order metadata normalizes symbol and protects terminal status transitions")
  void orderMetadataAndStateTransitions() {
    LimitOrder order = new LimitOrder("  msft ", true, 15, 400.0);

    assertNotNull(order.getOrderId());
    assertNotNull(order.getCreatedAt());
    assertEquals("MSFT", order.getSymbol());
    assertEquals(15, order.getQuantity());
    assertEquals(400.0, order.getLimitPrice(), 0.001);
    assertEquals(OrderStatus.PENDING, order.getStatus());

    order.setStatus(OrderStatus.CANCELLED);
    assertEquals(OrderStatus.CANCELLED, order.getStatus());
    assertThrows(IllegalStateException.class, () -> order.setStatus(OrderStatus.FILLED));
  }

  @Test
  @DisplayName("Order constructors reject invalid symbol, quantity, or limit price")
  void orderConstructorValidation() {
    assertThrows(IllegalArgumentException.class, () -> new MarketOrder("", true, 10));
    assertThrows(IllegalArgumentException.class, () -> new MarketOrder("AAPL", true, 0));
    assertThrows(IllegalArgumentException.class, () -> new MarketOrder("AAPL", true, -5));
    assertThrows(IllegalArgumentException.class, () -> new LimitOrder("AAPL", true, 10, 0.0));
    assertThrows(IllegalArgumentException.class, () -> new LimitOrder("AAPL", true, 10, -50.0));
    assertThrows(
        IllegalArgumentException.class, () -> new LimitOrder("AAPL", true, 10, Double.NaN));
  }
}