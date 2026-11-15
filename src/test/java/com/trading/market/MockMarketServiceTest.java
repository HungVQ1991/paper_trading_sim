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

class MockMarketServiceTest {

  @Test
  @DisplayName("Should emit at least 2 quotes within 2 seconds with valid price fluctuations")
  void testMockMarketServiceEmissionAndFluctuation() throws InterruptedException {
    MockMarketService mockService = new MockMarketService(100L);
    List<Quote> receivedQuotes = new CopyOnWriteArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);

    mockService.subscribe("AAPL");
    mockService.addListener(quote -> {
      if ("AAPL".equals(quote.getSymbol())) {
        receivedQuotes.add(quote);
        latch.countDown();
      }
    });

    mockService.start();
    assertTrue(mockService.isConnected(), "MockMarketService should be connected after start");

    boolean completed = latch.await(3, TimeUnit.SECONDS);
    assertTrue(completed, "Should receive at least 2 quotes within the timeout period");
    assertTrue(receivedQuotes.size() >= 2);

    for (Quote quote : receivedQuotes) {
      assertEquals("AAPL", quote.getSymbol());
      assertTrue(quote.getPrice() > 0, "Price must be strictly positive");
      assertTrue(quote.getPrice() >= 171.0 && quote.getPrice() <= 189.0,
          "Price " + quote.getPrice() + " should be within +/- 5% range of 180.0");
    }

    mockService.stop();
    assertFalse(mockService.isConnected(), "MockMarketService should be disconnected after stop");
  }

  @Test
  @DisplayName("Should maintain latest quote in cache for subscribed symbols")
  void testGetLatestQuote() {
    MockMarketService mockService = new MockMarketService();
    mockService.subscribe("MSFT");

    Quote quote = mockService.getLatestQuote("MSFT");
    assertNotNull(quote);
    assertEquals("MSFT", quote.getSymbol());
    assertEquals(410.0, quote.getPrice(), 0.001);
  }

  @Test
  @DisplayName("Should produce reproducible deterministic price stream with fixed seed")
  void testDeterministicSeed() throws InterruptedException {
    MockMarketService service1 = new MockMarketService(50L, 12345L);
    MockMarketService service2 = new MockMarketService(50L, 12345L);

    List<Double> prices1 = new CopyOnWriteArrayList<>();
    List<Double> prices2 = new CopyOnWriteArrayList<>();
    CountDownLatch latch1 = new CountDownLatch(3);
    CountDownLatch latch2 = new CountDownLatch(3);

    service1.subscribe("AAPL");
    service2.subscribe("AAPL");

    service1.addListener(q -> {
      if ("AAPL".equals(q.getSymbol()) && prices1.size() < 3) {
        prices1.add(q.getPrice());
        latch1.countDown();
      }
    });

    service2.addListener(q -> {
      if ("AAPL".equals(q.getSymbol()) && prices2.size() < 3) {
        prices2.add(q.getPrice());
        latch2.countDown();
      }
    });

    service1.start();
    service2.start();

    boolean done1 = latch1.await(3, TimeUnit.SECONDS);
    boolean done2 = latch2.await(3, TimeUnit.SECONDS);

    service1.stop();
    service2.stop();

    assertTrue(done1 && done2);
    assertEquals(prices1, prices2, "Prices with identical seed must match identically");
  }

  @Test
  @DisplayName("Should allow setting fixed price directly for test execution")
  void testSetPrice() {
    MockMarketService service = new MockMarketService();
    List<Quote> received = new CopyOnWriteArrayList<>();
    service.addListener(received::add);
    service.start();

    service.setPrice("NVDA", 125.50);

    Quote latest = service.getLatestQuote("NVDA");
    assertNotNull(latest);
    assertEquals("NVDA", latest.getSymbol());
    assertEquals(125.50, latest.getPrice(), 0.001);
    assertFalse(received.isEmpty());
    assertEquals(125.50, received.get(received.size() - 1).getPrice(), 0.001);

    service.stop();
  }

  @Test
  @DisplayName("Should stop emitting for unsubscribed symbols")
  void testUnsubscribe() {
    MockMarketService service = new MockMarketService();
    service.subscribe("TSLA");
    assertTrue(service.getSubscribedSymbols().contains("TSLA"));

    service.unsubscribe("TSLA");
    assertFalse(service.getSubscribedSymbols().contains("TSLA"));
  }
}
