package com.trading.market;

import com.trading.model.Quote;

public interface MarketService {
  void start();

  void stop();

  void subscribe(String symbol);

  void unsubscribe(String symbol);

  Quote getLatestQuote(String symbol);

  void addListener(QuoteListener listener);

  void removeListener(QuoteListener listener);

  MarketProviderType getProviderType();

  boolean isConnected();
}
