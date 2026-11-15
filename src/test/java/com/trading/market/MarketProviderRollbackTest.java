package com.trading.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trading.model.Quote;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarketProviderRollbackTest {

  @Test
  @DisplayName("Should reject provider switch when required API key is missing and preserve active provider")
  void testSwitchMissingApiKeyFailsValidationAndPreservesActiveProvider() {
    MarketServiceProviderManager manager = new MarketServiceProviderManager();
    MockMarketService mockService = new MockMarketService(100L);
    TwelveDataWebSocket twelveDataService = new TwelveDataWebSocket("");

    manager.registerProvider(MarketProviderType.MOCK_ENGINE, mockService);
    manager.registerProvider(MarketProviderType.TWELVE_DATA, twelveDataService);

    manager.switchProvider(MarketProviderType.MOCK_ENGINE, "");
    assertTrue(mockService.isConnected(), "Mock provider should be active");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        manager.switchProvider(MarketProviderType.TWELVE_DATA, "   "));

    assertTrue(ex.getMessage().contains("API key is required"),
        "Error message should mention missing API key");
    assertEquals(MarketProviderType.MOCK_ENGINE, manager.getActiveProviderType(),
        "Active provider must remain MockMarketService");
    assertTrue(mockService.isConnected(), "Active mock service must remain running");
  }

  @Test
  @DisplayName("Should automatically fallback to Mock engine when target provider start throws exception")
  void testSwitchFailureTriggersAutomaticFallbackToMock() {
    MarketServiceProviderManager manager = new MarketServiceProviderManager();
    MockMarketService mockService = new MockMarketService(100L);

    MarketService faultyService = new MarketService() {
      @Override
      public void start() {
        throw new RuntimeException("Simulated connection failure to remote exchange");
      }

      @Override
      public void stop() {
      }

      @Override
      public void subscribe(String symbol) {
      }

      @Override
      public void unsubscribe(String symbol) {
      }

      @Override
      public Quote getLatestQuote(String symbol) {
        return null;
      }

      @Override
      public void addListener(QuoteListener listener) {
      }

      @Override
      public void removeListener(QuoteListener listener) {
      }

      @Override
      public MarketProviderType getProviderType() {
        return MarketProviderType.FINNHUB;
      }

      @Override
      public boolean isConnected() {
        return false;
      }
    };

    manager.registerProvider(MarketProviderType.MOCK_ENGINE, mockService);
    manager.registerProvider(MarketProviderType.FINNHUB, faultyService);

    manager.subscribe("AAPL");
    manager.switchProvider(MarketProviderType.MOCK_ENGINE, "");

    List<MarketProviderType> notifications = new ArrayList<>();
    manager.addProviderChangeListener(notifications::add);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        manager.switchProvider(MarketProviderType.FINNHUB, "any-valid-key"));

    assertTrue(ex.getMessage().contains("Fallback to Mock engine"),
        "Exception message should mention fallback to Mock");
    assertEquals(MarketProviderType.MOCK_ENGINE, manager.getActiveProviderType(),
        "Active provider type must be MOCK_ENGINE after fallback");
    assertTrue(manager.getActiveService().isConnected(),
        "Fallback Mock service must be connected and running");
    assertTrue(manager.getSubscribedSymbols().contains("AAPL"),
        "Subscribed symbols must be retained after fallback");
  }

  @Test
  @DisplayName("Should not duplicate quotes when same listener is added multiple times")
  void testListenerDeduplication() {
    MarketServiceProviderManager manager = new MarketServiceProviderManager();
    MockMarketService mockService = new MockMarketService(100L);
    manager.registerProvider(MarketProviderType.MOCK_ENGINE, mockService);
    manager.switchProvider(MarketProviderType.MOCK_ENGINE, "");

    AtomicInteger callCount = new AtomicInteger(0);
    QuoteListener listener = quote -> callCount.incrementAndGet();

    manager.addGlobalListener(listener);
    manager.addGlobalListener(listener); // duplicate add

    mockService.setPrice("AAPL", 185.0);

    assertEquals(1, callCount.get(), "Listener should only receive quote once");
  }
}
