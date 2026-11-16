package com.trading.model;

/**
 * Represents the lifecycle state of a trading order.
 */
public enum OrderStatus {
  PENDING,
  FILLED,
  CANCELLED,
  REJECTED
}