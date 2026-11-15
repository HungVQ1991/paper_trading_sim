package com.trading.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trading.model.Quote;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarketProviderSwitchingTest {

  @Test
  @DisplayName("Should seamlessly switch provider at runtime without losing registered observers")
  void testDynamicProviderSwitching() throws InterruptedException {
    MarketServiceProviderManager manager = new MarketServiceProviderManager();

    MockMarketService mock1 = new MockMarketService(100L);
    MockMarketService mock2 = new MockMarketService(100L);

    manager.registerProvider(MarketProviderType.MOCK_ENGINE, mock1);
    manager.registerProvider(MarketProviderType.FINNHUB, mock2);

    manager.subscribe("AAPL");

    List<Quote> received = new CopyOnWriteArrayList<>();
    CountDownLatch firstLatch = new CountDownLatch(2);

    manager.addGlobalListener(quote -> {
      received.add(quote);
      firstLatch.countDown();
    });

    manager.switchProvider(MarketProviderType.MOCK_ENGINE, "");

    assertTrue(mock1.isConnected(), "Original provider should be running");
    assertFalse(mock2.isConnected(), "Target provider should not yet be running");
    assertEquals(MarketProviderType.MOCK_ENGINE, manager.getActiveProviderType());

    boolean firstReceived = firstLatch.await(3, TimeUnit.SECONDS);
    assertTrue(firstReceived, "Global observer should receive quotes from initial provider");
    assertTrue(received.size() >= 2);

    CountDownLatch secondLatch = new CountDownLatch(2);
    manager.addGlobalListener(quote -> secondLatch.countDown());

    manager.switchProvider(MarketProviderType.FINNHUB, "dummy-finnhub-key");

    assertFalse(mock1.isConnected(), "Previous provider must be stopped");
    assertTrue(mock2.isConnected(), "New provider must be started");
    assertEquals(MarketProviderType.FINNHUB, manager.getActiveProviderType());

    boolean secondReceived = secondLatch.await(3, TimeUnit.SECONDS);
    assertTrue(secondReceived, "Global observer must receive quotes from newly switched provider");

    assertTrue(manager.getSubscribedSymbols().contains("AAPL"));
    Quote latest = manager.getLatestQuote("AAPL");
    assertNotNull(latest);
    assertEquals("AAPL", latest.getSymbol());

    manager.getActiveService().stop();
    assertFalse(mock2.isConnected());
  }
}
