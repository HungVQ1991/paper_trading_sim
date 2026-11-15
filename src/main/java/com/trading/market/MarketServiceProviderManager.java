package com.trading.market;

import com.trading.model.Quote;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MarketServiceProviderManager {

  private static final Logger LOGGER = Logger.getLogger(
      MarketServiceProviderManager.class.getName());

  private MarketService activeService;
  private MarketProviderType activeProviderType;
  private final Map<MarketProviderType, MarketService> providers = new ConcurrentHashMap<>();
  private final Set<QuoteListener> globalListeners = new CopyOnWriteArraySet<>();
  private final Set<String> subscribedSymbols = new CopyOnWriteArraySet<>();
  private final List<Consumer<MarketProviderType>> providerChangeListeners =
      new CopyOnWriteArrayList<>();

  public synchronized void registerProvider(MarketProviderType type, MarketService service) {
    if (type == null || service == null) {
      throw new IllegalArgumentException("Provider type and service cannot be null");
    }
    providers.put(type, service);
    LOGGER.info("Registered market provider: " + type.getDisplayName());
  }

  public synchronized void switchProvider(MarketProviderType newType, String apiKey) {
    if (newType == null) {
      throw new IllegalArgumentException("Target provider type cannot be null");
    }

    MarketService newService = providers.get(newType);
    if (newService == null) {
      throw new IllegalArgumentException("No provider registered for type: " + newType);
    }

    // Pre-validation: do not disrupt healthy active provider if credentials are missing
    String key = apiKey != null ? apiKey.trim() : "";
    if (newType == MarketProviderType.TWELVE_DATA) {
      String existingKey = newService instanceof TwelveDataWebSocket
          ? ((TwelveDataWebSocket) newService).getApiKey() : "";
      if (key.isEmpty() && (existingKey == null || existingKey.isEmpty())) {
        throw new IllegalArgumentException("API key is required for provider: "
            + newType.getDisplayName());
      }
    } else if (newType == MarketProviderType.FINNHUB) {
      String existingKey = newService instanceof FinnhubWebSocket
          ? ((FinnhubWebSocket) newService).getApiKey() : "";
      if (key.isEmpty() && (existingKey == null || existingKey.isEmpty())) {
        throw new IllegalArgumentException("API key is required for provider: "
            + newType.getDisplayName());
      }
    }

    if (activeService != null) {
      for (QuoteListener listener : globalListeners) {
        activeService.removeListener(listener);
      }
      activeService.stop();
      LOGGER.info("Stopped previous provider: " + activeService.getProviderType());
    }

    if (!key.isEmpty()) {
      if (newService instanceof TwelveDataWebSocket) {
        ((TwelveDataWebSocket) newService).setApiKey(key);
      } else if (newService instanceof FinnhubWebSocket) {
        ((FinnhubWebSocket) newService).setApiKey(key);
      }
    }

    try {
      this.activeService = newService;
      this.activeProviderType = newType;

      for (QuoteListener listener : globalListeners) {
        newService.addListener(listener);
      }

      newService.start();
      LOGGER.info("Started new active provider: " + newType.getDisplayName());

      for (String symbol : subscribedSymbols) {
        newService.subscribe(symbol);
      }

      notifyProviderChange(newType);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Failed to start target provider " + newType
          + ". Falling back to MockMarketService.", ex);
      fallbackToMock();
      throw new IllegalStateException("Failed to activate provider "
          + newType.getDisplayName() + ". Fallback to Mock engine.", ex);
    }
  }

  public synchronized void fallbackToMock() {
    LOGGER.warning("Executing automatic fallback to MockMarketService.");
    if (activeService != null && activeService.getProviderType() != MarketProviderType.MOCK_ENGINE) {
      for (QuoteListener listener : globalListeners) {
        activeService.removeListener(listener);
      }
      activeService.stop();
    }

    MarketService mockService = providers.get(MarketProviderType.MOCK_ENGINE);
    if (mockService == null) {
      mockService = new MockMarketService();
      providers.put(MarketProviderType.MOCK_ENGINE, mockService);
    }

    this.activeService = mockService;
    this.activeProviderType = MarketProviderType.MOCK_ENGINE;

    for (QuoteListener listener : globalListeners) {
      mockService.addListener(listener);
    }

    mockService.start();
    for (String symbol : subscribedSymbols) {
      mockService.subscribe(symbol);
    }

    notifyProviderChange(MarketProviderType.MOCK_ENGINE);
  }

  public synchronized void subscribe(String symbol) {
    if (symbol == null || symbol.trim().isEmpty()) {
      return;
    }
    String upper = symbol.toUpperCase().trim();
    subscribedSymbols.add(upper);
    if (activeService != null) {
      activeService.subscribe(upper);
    }
  }

  public synchronized void unsubscribe(String symbol) {
    if (symbol == null || symbol.trim().isEmpty()) {
      return;
    }
    String upper = symbol.toUpperCase().trim();
    subscribedSymbols.remove(upper);
    if (activeService != null) {
      activeService.unsubscribe(upper);
    }
  }

  public synchronized void addGlobalListener(QuoteListener listener) {
    if (listener != null) {
      globalListeners.add(listener);
      if (activeService != null) {
        activeService.addListener(listener);
      }
    }
  }

  public synchronized void addListener(QuoteListener listener) {
    addGlobalListener(listener);
  }

  public synchronized void removeGlobalListener(QuoteListener listener) {
    if (listener != null) {
      globalListeners.remove(listener);
      if (activeService != null) {
        activeService.removeListener(listener);
      }
    }
  }

  public synchronized void removeListener(QuoteListener listener) {
    removeGlobalListener(listener);
  }

  public MarketService getActiveService() {
    return activeService;
  }

  public MarketProviderType getActiveProviderType() {
    if (activeProviderType != null) {
      return activeProviderType;
    }
    return activeService != null ? activeService.getProviderType() : null;
  }

  public Quote getLatestQuote(String symbol) {
    return activeService != null ? activeService.getLatestQuote(symbol) : null;
  }

  public Set<String> getSubscribedSymbols() {
    return Collections.unmodifiableSet(subscribedSymbols);
  }

  public Set<QuoteListener> getGlobalListeners() {
    return Collections.unmodifiableSet(globalListeners);
  }

  public Map<MarketProviderType, MarketService> getProviders() {
    return Collections.unmodifiableMap(providers);
  }

  public void addProviderChangeListener(Consumer<MarketProviderType> listener) {
    if (listener != null) {
      providerChangeListeners.add(listener);
    }
  }

  public void removeProviderChangeListener(Consumer<MarketProviderType> listener) {
    if (listener != null) {
      providerChangeListeners.remove(listener);
    }
  }

  private void notifyProviderChange(MarketProviderType type) {
    for (Consumer<MarketProviderType> listener : providerChangeListeners) {
      try {
        listener.accept(type);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error notifying provider change listener", ex);
      }
    }
  }
}
