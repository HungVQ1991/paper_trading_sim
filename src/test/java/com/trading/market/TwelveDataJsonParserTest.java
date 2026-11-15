package com.trading.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.trading.model.Quote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TwelveDataJsonParserTest {

  @Test
  @DisplayName("Should successfully parse standard Twelve Data price message")
  void testParseValidPriceMessage() {
    String json = "{\"event\": \"price\", \"symbol\": \"AAPL\", "
        + "\"price\": 182.45, \"timestamp\": 1711105400}";

    Quote quote = TwelveDataWebSocket.parseQuote(json);

    assertNotNull(quote);
    assertEquals("AAPL", quote.getSymbol());
    assertEquals(182.45, quote.getPrice(), 0.001);
    assertEquals(1711105400L, quote.getTimestamp().getEpochSecond());
  }

  @Test
  @DisplayName("Should parse Twelve Data message with change percent")
  void testParsePriceWithChangePercent() {
    String json = "{\"event\": \"price\", \"symbol\": \"msft\", "
        + "\"price\": 420.50, \"change_percent\": 1.25, \"timestamp\": 1711105400}";

    Quote quote = TwelveDataWebSocket.parseQuote(json);

    assertNotNull(quote);
    assertEquals("MSFT", quote.getSymbol());
    assertEquals(420.50, quote.getPrice(), 0.001);
    assertEquals(1.25, quote.getChangePercent(), 0.001);
  }

  @Test
  @DisplayName("Should return null for non-price events")
  void testIgnoreNonPriceEvent() {
    String json = "{\"event\": \"subscribe-status\", \"status\": \"ok\"}";
    Quote quote = TwelveDataWebSocket.parseQuote(json);
    assertNull(quote);
  }

  @Test
  @DisplayName("Should return null for invalid or null payload")
  void testInvalidJsonPayload() {
    assertNull(TwelveDataWebSocket.parseQuote(null));
    assertNull(TwelveDataWebSocket.parseQuote(""));
    assertNull(TwelveDataWebSocket.parseQuote("{not valid json"));
  }
}
