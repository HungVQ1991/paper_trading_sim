package com.trading.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for weighted-average cost basis, partial sales, realized P&amp;L, and valuation.
 */
class PositionCalculationTest {

  private static final double DELTA = 0.001;

  @Test
  @DisplayName("Weighted average price updates across multiple buys and remains unchanged on sell")
  void weightedAverageCostAndPartialSaleRealizedPnL() {
    Position position = new Position("AAPL");
    assertEquals(0, position.getQuantity());
    assertEquals(0.0, position.getAveragePrice(), DELTA);

    position.buyMore(10, 100.0);
    position.buyMore(10, 150.0);

    assertEquals(20, position.getQuantity());
    assertEquals(125.0, position.getAveragePrice(), DELTA);

    double realizedPnL = position.sell(10, 140.0);
    assertEquals(150.0, realizedPnL, DELTA);
    assertEquals(10, position.getQuantity());
    assertEquals(125.0, position.getAveragePrice(), DELTA);
  }

  @Test
  @DisplayName("Unrealized PnL and market value reflect current market price accurately")
  void unrealizedPnLAndMarketValue() {
    Position position = new Position("MSFT");
    position.buyMore(10, 400.0);

    assertEquals(4200.0, position.getMarketValue(420.0), DELTA);
    assertEquals(200.0, position.getUnrealizedPnL(420.0), DELTA);
    assertEquals(-150.0, position.getUnrealizedPnL(385.0), DELTA);
  }

  @Test
  @DisplayName("Full position sale resets quantity and average cost to zero")
  void fullSaleResetsAveragePrice() {
    Position position = new Position("TSLA", 5, 200.0);
    double realizedPnL = position.sell(5, 180.0);

    assertEquals(-100.0, realizedPnL, DELTA);
    assertEquals(0, position.getQuantity());
    assertEquals(0.0, position.getAveragePrice(), DELTA);
  }

  @Test
  @DisplayName("Selling more shares than owned or passing invalid inputs throws exception")
  void rejectOversellAndInvalidInputs() {
    Position position = new Position("AAPL", 10, 150.0);

    assertThrows(IllegalArgumentException.class, () -> position.sell(15, 160.0));
    assertEquals(10, position.getQuantity());
    assertEquals(150.0, position.getAveragePrice(), DELTA);

    assertThrows(IllegalArgumentException.class, () -> position.buyMore(0, 150.0));
    assertThrows(IllegalArgumentException.class, () -> position.buyMore(5, -10.0));
    assertThrows(IllegalArgumentException.class, () -> position.sell(0, 150.0));
    assertThrows(IllegalArgumentException.class, () -> position.sell(5, Double.NaN));
  }
}