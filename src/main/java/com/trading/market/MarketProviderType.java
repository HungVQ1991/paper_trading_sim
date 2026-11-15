package com.trading.market;

public enum MarketProviderType {
  TWELVE_DATA("Twelve Data (WebSocket)"),
  FINNHUB("Finnhub (WebSocket)"),
  ALPHA_VANTAGE("Alpha Vantage (REST)"),
  MOCK_ENGINE("Mô Phỏng Nội Tuyến (Mock Engine)");

  private final String displayName;

  MarketProviderType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
