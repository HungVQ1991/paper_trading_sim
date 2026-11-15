package com.trading.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trading.model.Quote;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FinnhubJsonParserTest {

  @Test
  @DisplayName("Should parse standard Finnhub trade JSON array")
  void testParseStandardTradeMessage() {
    String json = "{\n"
        + "  \"type\": \"trade\",\n"
        + "  \"data\": [\n"
        + "    {\"s\": \"AAPL\", \"p\": 182.45, \"t\": 1711105400000, \"v\": 100}\n"
        + "  ]\n"
        + "}";

    List<Quote> quotes = FinnhubWebSocket.parseQuotes(json);

    assertNotNull(quotes);
    assertEquals(1, quotes.size());
    Quote quote = quotes.get(0);
    assertEquals("AAPL", quote.getSymbol());
    assertEquals(182.45, quote.getPrice(), 0.001);
    assertEquals(1711105400L, quote.getTimestamp().getEpochSecond());

    Quote single = FinnhubWebSocket.parseQuote(json);
    assertNotNull(single);
    assertEquals("AAPL", single.getSymbol());
    assertEquals(182.45, single.getPrice(), 0.001);
  }

  @Test
  @DisplayName("Should parse multi-trade Finnhub data array")
  void testParseMultipleTrades() {
    String json = "{\"type\":\"trade\",\"data\":["
        + "{\"s\":\"AAPL\",\"p\":182.45,\"t\":1711105400000,\"v\":100},"
        + "{\"s\":\"MSFT\",\"p\":415.20,\"t\":1711105400000,\"v\":50}"
        + "]}";

    List<Quote> quotes = FinnhubWebSocket.parseQuotes(json);

    assertEquals(2, quotes.size());
    assertEquals("AAPL", quotes.get(0).getSymbol());
    assertEquals(182.45, quotes.get(0).getPrice(), 0.001);
    assertEquals("MSFT", quotes.get(1).getSymbol());
    assertEquals(415.20, quotes.get(1).getPrice(), 0.001);
  }

  @Test
  @DisplayName("Should ignore non-trade events such as ping")
  void testIgnoreNonTradeEvent() {
    String json = "{\"type\": \"ping\"}";
    List<Quote> quotes = FinnhubWebSocket.parseQuotes(json);
    assertTrue(quotes.isEmpty());
    assertNull(FinnhubWebSocket.parseQuote(json));
  }

  @Test
  @DisplayName("Should return empty list for null or malformed JSON")
  void testMalformedJsonPayload() {
    assertTrue(FinnhubWebSocket.parseQuotes(null).isEmpty());
    assertTrue(FinnhubWebSocket.parseQuotes("").isEmpty());
    assertTrue(FinnhubWebSocket.parseQuotes("{invalid json").isEmpty());
    assertNull(FinnhubWebSocket.parseQuote(null));
  }
}
