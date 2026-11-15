package com.trading.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.model.Quote;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class TwelveDataWebSocket implements MarketService {

  private static final Logger LOGGER = Logger.getLogger(TwelveDataWebSocket.class.getName());
  private static final String DEFAULT_BASE_URL = "wss://ws.twelvedata.com/v1/quotes/price";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final URI customUri;
  private final Map<String, Quote> latestQuotes = new ConcurrentHashMap<>();
  private final Set<String> subscribedSymbols = new CopyOnWriteArraySet<>();
  private final Set<QuoteListener> listeners = new CopyOnWriteArraySet<>();

  private String apiKey;
  private WebSocketClient client;
  private volatile boolean connected = false;

  public TwelveDataWebSocket() {
    this("", null);
  }

  public TwelveDataWebSocket(String apiKey) {
    this(apiKey, null);
  }

  public TwelveDataWebSocket(String apiKey, URI serverUri) {
    this.apiKey = apiKey != null ? apiKey.trim() : "";
    this.customUri = serverUri;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey != null ? apiKey.trim() : "";
  }

  public static String sanitizeUri(URI uri) {
    if (uri == null) {
      return "null";
    }
    return uri.toString().replaceAll("([?&]apikey=)[^&]+", "$1***");
  }

  @Override
  public synchronized void start() {
    if (isConnected()) {
      return;
    }

    URI targetUri = customUri;
    if (targetUri == null) {
      if (apiKey == null || apiKey.isEmpty()) {
        LOGGER.warning("Twelve Data API key is missing. Connection not initiated.");
        return;
      }
      targetUri = URI.create(DEFAULT_BASE_URL + "?apikey=" + apiKey);
    }

    try {
      client = new InnerWebSocketClient(targetUri);
      client.connect();
      LOGGER.info("TwelveDataWebSocket connecting to " + sanitizeUri(targetUri));
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Failed to initiate Twelve Data WebSocket connection", ex);
      connected = false;
    }
  }

  @Override
  public synchronized void stop() {
    connected = false;
    if (client != null) {
      try {
        client.close();
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error closing Twelve Data WebSocket", ex);
      }
      client = null;
    }
    LOGGER.info("TwelveDataWebSocket stopped.");
  }

  @Override
  public void subscribe(String symbol) {
    if (symbol == null || symbol.trim().isEmpty()) {
      return;
    }
    String upper = symbol.toUpperCase().trim();
    subscribedSymbols.add(upper);

    if (isConnected() && client != null) {
      sendSubscribeMessage(upper);
    }
  }

  @Override
  public void unsubscribe(String symbol) {
    if (symbol == null || symbol.trim().isEmpty()) {
      return;
    }
    String upper = symbol.toUpperCase().trim();
    subscribedSymbols.remove(upper);

    if (isConnected() && client != null) {
      sendUnsubscribeMessage(upper);
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
    return MarketProviderType.TWELVE_DATA;
  }

  @Override
  public boolean isConnected() {
    return connected && client != null && client.isOpen();
  }

  public Set<String> getSubscribedSymbols() {
    return Collections.unmodifiableSet(subscribedSymbols);
  }

  public static Quote parseQuote(String jsonMessage) {
    if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
      return null;
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(jsonMessage);
      if (node.has("event") && !"price".equalsIgnoreCase(node.get("event").asText())) {
        return null;
      }
      if (!node.has("symbol") || !node.has("price")) {
        return null;
      }

      String symbol = node.get("symbol").asText();
      double price = node.get("price").asDouble();
      if (price <= 0.0 || Double.isNaN(price) || Double.isInfinite(price)) {
        return null;
      }

      double changePercent = 0.0;
      if (node.has("change_percent")) {
        changePercent = node.get("change_percent").asDouble();
      } else if (node.has("percent_change")) {
        changePercent = node.get("percent_change").asDouble();
      }

      Instant timestamp = Instant.now();
      if (node.has("timestamp")) {
        long rawTs = node.get("timestamp").asLong();
        if (rawTs > 100_000_000_000L) {
          timestamp = Instant.ofEpochMilli(rawTs);
        } else if (rawTs > 0) {
          timestamp = Instant.ofEpochSecond(rawTs);
        }
      }

      return new Quote(symbol, price, changePercent, timestamp);
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Error parsing Twelve Data JSON: " + jsonMessage, ex);
      return null;
    }
  }

  private void sendSubscribeMessage(String symbol) {
    String payload = "{\"action\": \"subscribe\", \"params\": {\"symbols\": \"" + symbol + "\"}}";
    try {
      client.send(payload);
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Failed to send subscribe for: " + symbol, ex);
    }
  }

  private void sendUnsubscribeMessage(String symbol) {
    String payload = "{\"action\": \"unsubscribe\", \"params\": {\"symbols\": \""
        + symbol + "\"}}";
    try {
      client.send(payload);
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Failed to send unsubscribe for: " + symbol, ex);
    }
  }

  private void dispatchQuote(Quote quote) {
    for (QuoteListener listener : listeners) {
      try {
        listener.onPriceUpdate(quote);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error notifying QuoteListener in TwelveDataWebSocket", ex);
      }
    }
  }

  private class InnerWebSocketClient extends WebSocketClient {

    public InnerWebSocketClient(URI serverUri) {
      super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
      connected = true;
      LOGGER.info("Twelve Data WebSocket connected.");
      for (String symbol : subscribedSymbols) {
        sendSubscribeMessage(symbol);
      }
    }

    @Override
    public void onMessage(String message) {
      Quote quote = parseQuote(message);
      if (quote != null) {
        latestQuotes.put(quote.getSymbol(), quote);
        dispatchQuote(quote);
      }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
      connected = false;
      LOGGER.info("Twelve Data WebSocket closed. Code: " + code + ", Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
      LOGGER.log(Level.WARNING, "Twelve Data WebSocket error: " + ex.getMessage(), ex);
    }
  }
}
