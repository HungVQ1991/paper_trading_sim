package com.trading.sim.ui;

import java.text.NumberFormat;
import java.util.Locale;

/** Validates display-layer order input without depending on JavaFX controls. */
public final class OrderFormValidator {

  /** Validates the form fields required to preview an order. */
  public ValidationResult validate(String orderType, String quantity, String limitPrice) {
    if (!isPositiveWholeNumber(quantity)) {
      return new ValidationResult(false, "Quantity must be a positive whole number.");
    }
    if ("LIMIT".equals(orderType) && !isPositiveNumber(limitPrice)) {
      return new ValidationResult(false, "Limit price must be greater than zero.");
    }
    return new ValidationResult(true, "");
  }

  /** Formats the current order value in US currency. */
  public String formatEstimatedCost(String quantity, double unitPrice) {
    long parsedQuantity = Long.parseLong(quantity.trim());
    NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
    return currency.format(parsedQuantity * unitPrice);
  }

  private boolean isPositiveWholeNumber(String value) {
    try {
      return Long.parseLong(value.trim()) > 0;
    } catch (NumberFormatException | NullPointerException exception) {
      return false;
    }
  }

  private boolean isPositiveNumber(String value) {
    try {
      double parsedValue = Double.parseDouble(value.trim());
      return Double.isFinite(parsedValue) && parsedValue > 0;
    } catch (NumberFormatException | NullPointerException exception) {
      return false;
    }
  }

  /** The result of one validation pass. */
  public record ValidationResult(boolean isValid, String message) {
  }
}
