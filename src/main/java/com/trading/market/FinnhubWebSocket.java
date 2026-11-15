package com.trading.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.model.Quote;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class FinnhubWebSocket implements MarketService {

  private static final Logger LOGGER = Logger.getLogger(FinnhubWebSocket.class.getName());
  private static final String DEFAULT_BASE_URL = "wss://ws.finnhub.io";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final URI customUri;
  private final Map<String, Quote> latestQuotes = new ConcurrentHashMap<>();
  private final Set<String> subscribedSymbols = new CopyOnWriteArraySet<>();
  private final Set<QuoteListener> listeners = new CopyOnWriteArraySet<>();

  private String apiKey;
  private WebSocketClient client;
  private volatile boolean connected = false;

  public FinnhubWebSocket() {
    this("", null);
  }

  public FinnhubWebSocket(String apiKey) {
    this(apiKey, null);
  }

  public FinnhubWebSocket(String apiKey, URI serverUri) {
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
    return uri.toString().replaceAll("([?&]token=)[^&]+", "$1***");
  }

  @Override
  public synchronized void start() {
    if (isConnected()) {
      return;
    }

    URI targetUri = customUri;
    if (targetUri == null) {
      if (apiKey == null || apiKey.isEmpty()) {
        LOGGER.warning("Finnhub API key is missing. Connection not initiated.");
        return;
      }
      targetUri = URI.create(DEFAULT_BASE_URL + "?token=" + apiKey);
    }

    try {
      client = new InnerWebSocketClient(targetUri);
      client.connect();
      LOGGER.info("FinnhubWebSocket connecting to " + sanitizeUri(targetUri));
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Failed to initiate Finnhub WebSocket connection", ex);
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
        LOGGER.log(Level.WARNING, "Error closing Finnhub WebSocket", ex);
      }
      client = null;
    }
    LOGGER.info("FinnhubWebSocket stopped.");
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
    return MarketProviderType.FINNHUB;
  }

  @Override
  public boolean isConnected() {
    return connected && client != null && client.isOpen();
  }

  public Set<String> getSubscribedSymbols() {
    return Collections.unmodifiableSet(subscribedSymbols);
  }

  public static List<Quote> parseQuotes(String jsonMessage) {
    if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
      return Collections.emptyList();
    }
    try {
      JsonNode root = OBJECT_MAPPER.readTree(jsonMessage);
      if (!root.has("type") || !"trade".equalsIgnoreCase(root.get("type").asText())) {
        return Collections.emptyList();
      }
      JsonNode dataNode = root.get("data");
      if (dataNode == null || !dataNode.isArray()) {
        return Collections.emptyList();
      }

      List<Quote> quotes = new ArrayList<>();
      for (JsonNode item : dataNode) {
        if (item.has("s") && item.has("p")) {
          String symbol = item.get("s").asText();
          double price = item.get("p").asDouble();
          if (price <= 0.0 || Double.isNaN(price) || Double.isInfinite(price)) {
            continue;
          }
          Instant timestamp = Instant.now();
          if (item.has("t")) {
            long rawTs = item.get("t").asLong();
            if (rawTs > 100_000_000_000L) {
              timestamp = Instant.ofEpochMilli(rawTs);
            } else if (rawTs > 0) {
              timestamp = Instant.ofEpochSecond(rawTs);
            }
          }
          quotes.add(new Quote(symbol, price, 0.0, timestamp));
        }
      }
      return quotes;
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Error parsing Finnhub JSON: " + jsonMessage, ex);
      return Collections.emptyList();
    }
  }

  public static Quote parseQuote(String jsonMessage) {
    List<Quote> quotes = parseQuotes(jsonMessage);
    return quotes.isEmpty() ? null : quotes.get(0);
  }

  private void sendSubscribeMessage(String symbol) {
    String payload = "{\"type\": \"subscribe\", \"symbol\": \"" + symbol + "\"}";
    try {
      client.send(payload);
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Failed to send subscribe for: " + symbol, ex);
    }
  }

  private void sendUnsubscribeMessage(String symbol) {
    String payload = "{\"type\": \"unsubscribe\", \"symbol\": \"" + symbol + "\"}";
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
        LOGGER.log(Level.WARNING, "Error notifying QuoteListener in FinnhubWebSocket", ex);
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
      LOGGER.info("Finnhub WebSocket connected.");
      for (String symbol : subscribedSymbols) {
        sendSubscribeMessage(symbol);
      }
    }

    @Override
    public void onMessage(String message) {
      List<Quote> quotes = parseQuotes(message);
      for (Quote quote : quotes) {
        latestQuotes.put(quote.getSymbol(), quote);
        dispatchQuote(quote);
      }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
      connected = false;
      LOGGER.info("Finnhub WebSocket closed. Code: " + code + ", Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
      LOGGER.log(Level.WARNING, "Finnhub WebSocket error: " + ex.getMessage(), ex);
    }
  }
}
