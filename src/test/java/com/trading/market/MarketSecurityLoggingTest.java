package com.trading.market;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarketSecurityLoggingTest {

  @Test
  @DisplayName("TwelveDataWebSocket URI sanitization must mask apikey query param")
  void testTwelveDataUriMasking() {
    URI uriWithKey = URI.create("wss://ws.twelvedata.com/v1/quotes/price?apikey=secret_key_12345");
    String sanitized = TwelveDataWebSocket.sanitizeUri(uriWithKey);

    assertFalse(sanitized.contains("secret_key_12345"), "Sanitized URI must not contain raw API key");
    assertTrue(sanitized.contains("apikey=***"), "Sanitized URI must mask apiKey with ***");
  }

  @Test
  @DisplayName("FinnhubWebSocket URI sanitization must mask token query param")
  void testFinnhubUriMasking() {
    URI uriWithToken = URI.create("wss://ws.finnhub.io?token=finnhub_secret_abc");
    String sanitized = FinnhubWebSocket.sanitizeUri(uriWithToken);

    assertFalse(sanitized.contains("finnhub_secret_abc"), "Sanitized URI must not contain raw token");
    assertTrue(sanitized.contains("token=***"), "Sanitized URI must mask token with ***");
  }
}
