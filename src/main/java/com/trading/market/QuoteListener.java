package com.trading.market;

import com.trading.model.Quote;

@FunctionalInterface
public interface QuoteListener {
  void onPriceUpdate(Quote quote);
}
