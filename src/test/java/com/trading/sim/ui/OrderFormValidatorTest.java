package com.trading.sim.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OrderFormValidatorTest {

  private final OrderFormValidator validator = new OrderFormValidator();

  @Test
  void rejectsInvalidQuantities() {
    assertFalse(validator.validate("MARKET", "", "").isValid());
    assertFalse(validator.validate("MARKET", "0", "").isValid());
    assertFalse(validator.validate("MARKET", "-5", "").isValid());
    assertFalse(validator.validate("MARKET", "1.5", "").isValid());
    assertFalse(validator.validate("MARKET", "shares", "").isValid());
  }

  @Test
  void acceptsMarketOrderWithoutLimitPrice() {
    assertTrue(validator.validate("MARKET", "10", "").isValid());
  }

  @Test
  void rejectsInvalidLimitPrice() {
    assertFalse(validator.validate("LIMIT", "10", "").isValid());
    assertFalse(validator.validate("LIMIT", "10", "0").isValid());
    assertFalse(validator.validate("LIMIT", "10", "-1").isValid());
    assertFalse(validator.validate("LIMIT", "10", "abc").isValid());
  }

  @Test
  void formatsEstimatedCost() {
    assertEquals("$1,500.00", validator.formatEstimatedCost("10", 150.0));
  }
}
