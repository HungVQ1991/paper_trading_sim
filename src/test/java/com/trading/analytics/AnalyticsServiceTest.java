package com.trading.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trading.model.Trade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AnalyticsService}. Plain JUnit: no Spring context, no network.
 */
class AnalyticsServiceTest {

  private static final double DELTA = 0.001;

  /** SELLs of +100, +50, +200 and -80: three wins out of four. */
  private static List<Trade> threeWinsOneLoss() {
    return List.of(
        TradeFixtures.sell("T-1", 100.0),
        TradeFixtures.sell("T-2", 50.0),
        TradeFixtures.sell("T-3", 200.0),
        TradeFixtures.sell("T-4", -80.0));
  }

  /** A BUY record that (unusually) carries a P&amp;L, to prove BUYs are ignored. */
  private static Trade buyWithPnL(String tradeId, double realizedPnL) {
    return TradeFixtures.trade(
        tradeId, TradeFixtures.DEFAULT_SYMBOL, true, TradeFixtures.DEFAULT_QUANTITY,
        TradeFixtures.DEFAULT_PRICE, realizedPnL, TradeFixtures.BASE_TIME);
  }

  // ---------------------------------------------------------------- win rate

  @Test
  void winRateIsSeventyFiveForThreeWinsOutOfFourSells() {
    assertEquals(75.0, AnalyticsService.calculateWinRate(threeWinsOneLoss()), DELTA);
  }

  @Test
  void winRateIsZeroForNullList() {
    assertEquals(0.0, AnalyticsService.calculateWinRate(null), DELTA);
  }

  @Test
  void winRateIsZeroForEmptyList() {
    assertEquals(0.0, AnalyticsService.calculateWinRate(List.of()), DELTA);
  }

  @Test
  void winRateIsZeroWhenThereAreNoSells() {
    List<Trade> onlyBuys = List.of(TradeFixtures.buy("T-1"), TradeFixtures.buy("T-2"));

    assertEquals(0.0, AnalyticsService.calculateWinRate(onlyBuys), DELTA);
  }

  @Test
  void winRateCountsBreakEvenSellAsNonWin() {
    List<Trade> trades = List.of(
        TradeFixtures.sell("T-1", 100.0),
        TradeFixtures.sell("T-2", 0.0));

    assertEquals(50.0, AnalyticsService.calculateWinRate(trades), DELTA);
  }

  @Test
  void winRateIsZeroWhenEverySellBreaksEven() {
    List<Trade> trades = List.of(
        TradeFixtures.sell("T-1", 0.0),
        TradeFixtures.sell("T-2", 0.0));

    assertEquals(0.0, AnalyticsService.calculateWinRate(trades), DELTA);
  }

  @Test
  void winRateIgnoresBuys() {
    List<Trade> trades = List.of(
        TradeFixtures.buy("T-1"),
        buyWithPnL("T-2", 999.0),
        TradeFixtures.sell("T-3", 100.0),
        TradeFixtures.sell("T-4", -40.0));

    assertEquals(50.0, AnalyticsService.calculateWinRate(trades), DELTA);
  }

  @Test
  void winRateRejectsNullElement() {
    List<Trade> trades = Arrays.asList(TradeFixtures.sell("T-1", 100.0), null);

    assertThrows(
        IllegalArgumentException.class, () -> AnalyticsService.calculateWinRate(trades));
  }

  // --------------------------------------------------------------- total P&L

  @Test
  void totalPnLSumsRealizedPnLOfSells() {
    assertEquals(270.0, AnalyticsService.calculateTotalPnL(threeWinsOneLoss()), DELTA);
  }

  @Test
  void totalPnLIsZeroForNullList() {
    assertEquals(0.0, AnalyticsService.calculateTotalPnL(null), DELTA);
  }

  @Test
  void totalPnLIsZeroForEmptyList() {
    assertEquals(0.0, AnalyticsService.calculateTotalPnL(List.of()), DELTA);
  }

  @Test
  void totalPnLIgnoresBuys() {
    List<Trade> trades = List.of(
        buyWithPnL("T-1", 999.0),
        TradeFixtures.sell("T-2", 55.0),
        TradeFixtures.sell("T-3", -15.5));

    assertEquals(39.5, AnalyticsService.calculateTotalPnL(trades), DELTA);
  }

  @Test
  void totalPnLRejectsNullElement() {
    List<Trade> trades = Arrays.asList(null, TradeFixtures.sell("T-1", 100.0));

    assertThrows(
        IllegalArgumentException.class, () -> AnalyticsService.calculateTotalPnL(trades));
  }

  // ------------------------------------------------------------ max drawdown

  @Test
  void maxDrawdownIsTwentyFiveForPeakOneTwentyThenNinety() {
    List<Double> equity = List.of(100000.0, 120000.0, 90000.0, 110000.0);

    assertEquals(25.0, AnalyticsService.calculateMaxDrawdown(equity), DELTA);
  }

  @Test
  void maxDrawdownIsZeroForNullList() {
    assertEquals(0.0, AnalyticsService.calculateMaxDrawdown(null), DELTA);
  }

  @Test
  void maxDrawdownIsZeroForEmptyList() {
    assertEquals(0.0, AnalyticsService.calculateMaxDrawdown(List.of()), DELTA);
  }

  @Test
  void maxDrawdownIsZeroForSinglePoint() {
    assertEquals(0.0, AnalyticsService.calculateMaxDrawdown(List.of(100000.0)), DELTA);
  }

  @Test
  void maxDrawdownIsZeroForFlatSeries() {
    List<Double> equity = List.of(100.0, 100.0, 100.0);

    assertEquals(0.0, AnalyticsService.calculateMaxDrawdown(equity), DELTA);
  }

  @Test
  void maxDrawdownIsZeroForRisingSeries() {
    List<Double> equity = List.of(100.0, 110.0, 125.0, 180.0);

    assertEquals(0.0, AnalyticsService.calculateMaxDrawdown(equity), DELTA);
  }

  @Test
  void maxDrawdownMeasuresFallingSeriesFromFirstPoint() {
    List<Double> equity = List.of(100.0, 80.0, 50.0);

    assertEquals(50.0, AnalyticsService.calculateMaxDrawdown(equity), DELTA);
  }

  @Test
  void maxDrawdownPicksLargestOfSeveralDeclines() {
    // First decline: 100 -> 90 is 10%. Second decline: 120 -> 60 is 50%.
    List<Double> equity = List.of(100.0, 90.0, 120.0, 60.0, 70.0);

    assertEquals(50.0, AnalyticsService.calculateMaxDrawdown(equity), DELTA);
  }

  @Test
  void maxDrawdownIsZeroWhenPeakIsNeverPositive() {
    assertEquals(0.0, AnalyticsService.calculateMaxDrawdown(List.of(0.0, 0.0)), DELTA);
    assertEquals(0.0, AnalyticsService.calculateMaxDrawdown(List.of(-100.0, -200.0)), DELTA);
  }

  @Test
  void maxDrawdownStartsMeasuringOnceThePeakBecomesPositive() {
    List<Double> equity = List.of(0.0, 100.0, 50.0);

    assertEquals(50.0, AnalyticsService.calculateMaxDrawdown(equity), DELTA);
  }

  @Test
  void maxDrawdownRejectsNonFiniteValues() {
    List<Double> withNaN = List.of(100.0, Double.NaN);
    List<Double> withInfinity = List.of(100.0, Double.POSITIVE_INFINITY);

    assertThrows(
        IllegalArgumentException.class, () -> AnalyticsService.calculateMaxDrawdown(withNaN));
    assertThrows(
        IllegalArgumentException.class,
        () -> AnalyticsService.calculateMaxDrawdown(withInfinity));
  }

  @Test
  void maxDrawdownRejectsNullElement() {
    List<Double> equity = Arrays.asList(100.0, null, 90.0);

    assertThrows(
        IllegalArgumentException.class, () -> AnalyticsService.calculateMaxDrawdown(equity));
  }

  @Test
  void maxDrawdownDoesNotModifyTheInputList() {
    List<Double> equity = new ArrayList<>(List.of(100.0, 120.0, 90.0));
    List<Double> before = List.copyOf(equity);

    AnalyticsService.calculateMaxDrawdown(equity);

    assertEquals(before, equity);
  }

  // ------------------------------------------------------- best / worst trade

  @Test
  void bestTradeHasHighestRealizedPnL() {
    Optional<Trade> best = AnalyticsService.getBestTrade(threeWinsOneLoss());

    assertTrue(best.isPresent());
    assertEquals("T-3", best.get().getTradeId());
  }

  @Test
  void worstTradeHasLowestRealizedPnL() {
    Optional<Trade> worst = AnalyticsService.getWorstTrade(threeWinsOneLoss());

    assertTrue(worst.isPresent());
    assertEquals("T-4", worst.get().getTradeId());
  }

  @Test
  void bestTradeTieIsWonByEarliestInListOrder() {
    Trade first = TradeFixtures.sell("T-1", 200.0);
    Trade second = TradeFixtures.sell("T-2", 200.0);
    List<Trade> trades = List.of(TradeFixtures.sell("T-0", 10.0), first, second);

    assertSame(first, AnalyticsService.getBestTrade(trades).orElseThrow());
  }

  @Test
  void worstTradeTieIsWonByEarliestInListOrder() {
    Trade first = TradeFixtures.sell("T-1", -80.0);
    Trade second = TradeFixtures.sell("T-2", -80.0);
    List<Trade> trades = List.of(TradeFixtures.sell("T-0", 10.0), first, second);

    assertSame(first, AnalyticsService.getWorstTrade(trades).orElseThrow());
  }

  @Test
  void bestAndWorstIgnoreBuys() {
    Trade onlySell = TradeFixtures.sell("T-2", 5.0);
    List<Trade> trades = List.of(buyWithPnL("T-1", 999.0), onlySell, buyWithPnL("T-3", -999.0));

    assertSame(onlySell, AnalyticsService.getBestTrade(trades).orElseThrow());
    assertSame(onlySell, AnalyticsService.getWorstTrade(trades).orElseThrow());
  }

  @Test
  void bestAndWorstAreEmptyWhenThereAreNoSells() {
    List<Trade> onlyBuys = List.of(TradeFixtures.buy("T-1"), TradeFixtures.buy("T-2"));

    assertTrue(AnalyticsService.getBestTrade(onlyBuys).isEmpty());
    assertTrue(AnalyticsService.getWorstTrade(onlyBuys).isEmpty());
  }

  @Test
  void bestAndWorstAreEmptyForNullOrEmptyList() {
    assertTrue(AnalyticsService.getBestTrade(null).isEmpty());
    assertTrue(AnalyticsService.getWorstTrade(null).isEmpty());
    assertTrue(AnalyticsService.getBestTrade(List.of()).isEmpty());
    assertTrue(AnalyticsService.getWorstTrade(List.of()).isEmpty());
  }

  @Test
  void bestAndWorstRejectNullElement() {
    List<Trade> trades = Arrays.asList(TradeFixtures.sell("T-1", 100.0), null);

    assertThrows(IllegalArgumentException.class, () -> AnalyticsService.getBestTrade(trades));
    assertThrows(IllegalArgumentException.class, () -> AnalyticsService.getWorstTrade(trades));
  }
}
