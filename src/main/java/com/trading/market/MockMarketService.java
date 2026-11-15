package com.trading.market;

import com.trading.model.Quote;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockMarketService implements MarketService {

  private static final Logger LOGGER = Logger.getLogger(MockMarketService.class.getName());
  private static final long DEFAULT_PERIOD_MS = 1000L;

  private final long periodMs;
  private final Random randomGenerator;
  private final Map<String, Double> basePrices = new ConcurrentHashMap<>();
  private final Map<String, Double> currentPrices = new ConcurrentHashMap<>();
  private final Map<String, Quote> latestQuotes = new ConcurrentHashMap<>();
  private final Set<String> subscribedSymbols = new CopyOnWriteArraySet<>();
  private final Set<QuoteListener> listeners = new CopyOnWriteArraySet<>();

  private ScheduledExecutorService scheduler;
  private volatile boolean connected = false;

  public MockMarketService() {
    this(DEFAULT_PERIOD_MS, (Random) null);
  }

  public MockMarketService(long periodMs) {
    this(periodMs, (Random) null);
  }

  public MockMarketService(long periodMs, long seed) {
    this(periodMs, new Random(seed));
  }

  public MockMarketService(long periodMs, Random randomGenerator) {
    this.periodMs = periodMs > 0 ? periodMs : DEFAULT_PERIOD_MS;
    this.randomGenerator = randomGenerator;
    initDefaultPrices();
  }

  private void initDefaultPrices() {
    basePrices.put("AAPL", 180.0);
    basePrices.put("MSFT", 410.0);
    basePrices.put("TSLA", 175.0);
    basePrices.put("GOOGL", 150.0);

    for (Map.Entry<String, Double> entry : basePrices.entrySet()) {
      currentPrices.put(entry.getKey(), entry.getValue());
      Quote quote = new Quote(entry.getKey(), entry.getValue(), 0.0, Instant.now());
      latestQuotes.put(entry.getKey(), quote);
    }
  }

  @Override
  public synchronized void start() {
    if (connected) {
      return;
    }
    connected = true;

    if (subscribedSymbols.isEmpty()) {
      subscribedSymbols.addAll(basePrices.keySet());
    }

    scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "mock-market-engine");
      thread.setDaemon(true);
      return thread;
    });

    scheduler.scheduleAtFixedRate(this::tick, 0, periodMs, TimeUnit.MILLISECONDS);
    LOGGER.info("MockMarketService started with interval " + periodMs + " ms.");
  }

  @Override
  public synchronized void stop() {
    connected = false;
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    LOGGER.info("MockMarketService stopped.");
  }

  @Override
  public void subscribe(String symbol) {
    if (symbol == null || symbol.trim().isEmpty()) {
      return;
    }
    String upper = symbol.toUpperCase().trim();
    subscribedSymbols.add(upper);
    basePrices.putIfAbsent(upper, 100.0);
    currentPrices.putIfAbsent(upper, basePrices.get(upper));

    Quote existing = latestQuotes.get(upper);
    if (existing == null) {
      double price = currentPrices.get(upper);
      Quote newQuote = new Quote(upper, price, 0.0, Instant.now());
      latestQuotes.put(upper, newQuote);
      if (connected) {
        dispatchQuote(newQuote);
      }
    } else if (connected) {
      dispatchQuote(existing);
    }
  }

  @Override
  public void unsubscribe(String symbol) {
    if (symbol != null) {
      subscribedSymbols.remove(symbol.toUpperCase().trim());
    }
  }

  @Override
  public Quote getLatestQuote(String symbol) {
    if (symbol == null) {
      return null;
    }
    return latestQuotes.get(symbol.toUpperCase().trim());
  }

  @Override
  public void addListener(QuoteListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeListener(QuoteListener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  @Override
  public MarketProviderType getProviderType() {
    return MarketProviderType.MOCK_ENGINE;
  }

  @Override
  public boolean isConnected() {
    return connected && scheduler != null && !scheduler.isShutdown();
  }

  public Set<String> getSubscribedSymbols() {
    return Collections.unmodifiableSet(subscribedSymbols);
  }

  public void setPrice(String symbol, double price) {
    if (symbol == null || symbol.trim().isEmpty()) {
      return;
    }
    if (price <= 0.0 || Double.isNaN(price) || Double.isInfinite(price)) {
      throw new IllegalArgumentException("Price must be positive and finite: " + price);
    }
    String upper = symbol.toUpperCase().trim();
    subscribedSymbols.add(upper);
    basePrices.putIfAbsent(upper, price);

    double roundedPrice = Math.round(price * 100.0) / 100.0;
    currentPrices.put(upper, roundedPrice);

    double base = basePrices.get(upper);
    double changePercent = ((roundedPrice - base) / base) * 100.0;
    changePercent = Math.round(changePercent * 100.0) / 100.0;

    Quote quote = new Quote(upper, roundedPrice, changePercent, Instant.now());
    latestQuotes.put(upper, quote);
    if (connected) {
      dispatchQuote(quote);
    }
  }

  private void tick() {
    for (String symbol : subscribedSymbols) {
      double base = basePrices.getOrDefault(symbol, 100.0);
      double current = currentPrices.getOrDefault(symbol, base);

      double randVal = randomGenerator != null
          ? randomGenerator.nextDouble()
          : ThreadLocalRandom.current().nextDouble();
      double delta = (randVal - 0.5) * 0.01;
      double newPrice = current * (1.0 + delta);
      if (newPrice < 0.01) {
        newPrice = 0.01;
      }
      newPrice = Math.round(newPrice * 100.0) / 100.0;
      currentPrices.put(symbol, newPrice);

      double changePercent = ((newPrice - base) / base) * 100.0;
      changePercent = Math.round(changePercent * 100.0) / 100.0;

      Quote quote = new Quote(symbol, newPrice, changePercent, Instant.now());
      latestQuotes.put(symbol, quote);
      dispatchQuote(quote);
    }
  }

  private void dispatchQuote(Quote quote) {
    for (QuoteListener listener : listeners) {
      try {
        listener.onPriceUpdate(quote);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error notifying QuoteListener in MockMarketService", ex);
      }
    }
  }
}
