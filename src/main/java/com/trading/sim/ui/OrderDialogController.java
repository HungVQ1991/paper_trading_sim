package com.trading.sim.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

/** Handles JavaFX order-entry controls before a trading engine is wired in. */
public final class OrderDialogController {

  private final OrderFormValidator validator = new OrderFormValidator();

  @FXML
  private ComboBox<String> symbolComboBox;

  @FXML
  private ComboBox<String> orderTypeComboBox;

  @FXML
  private RadioButton buyRadio;

  @FXML
  private TextField quantityField;

  @FXML
  private TextField limitPriceField;

  @FXML
  private Label estimatedCostLabel;

  @FXML
  private Label validationMessageLabel;

  @FXML
  private void initialize() {
    symbolComboBox.getItems().setAll("AAPL", "MSFT", "TSLA");
    symbolComboBox.getSelectionModel().selectFirst();
    orderTypeComboBox.getItems().setAll("MARKET", "LIMIT");
    orderTypeComboBox.getSelectionModel().select("MARKET");
    buyRadio.setSelected(true);
    quantityField.textProperty().addListener((observable, oldValue, newValue) -> updateEstimate());
    limitPriceField.textProperty().addListener((observable, oldValue, newValue) -> updateEstimate());
    symbolComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateEstimate());
    updateOrderType();
  }

  @FXML
  private void updateOrderType() {
    boolean isLimitOrder = "LIMIT".equals(orderTypeComboBox.getValue());
    limitPriceField.setDisable(!isLimitOrder);
    if (!isLimitOrder) {
      limitPriceField.clear();
    }
    updateEstimate();
  }

  @FXML
  private void confirmOrder() {
    OrderFormValidator.ValidationResult validation = validator.validate(
        orderTypeComboBox.getValue(), quantityField.getText(), limitPriceField.getText());
    if (!validation.isValid()) {
      validationMessageLabel.setText(validation.message());
      return;
    }
    validationMessageLabel.setText(
        "Order preview is valid. Connect the trading engine to submit it.");
  }

  @FXML
  private void closeDialog() {
    quantityField.getScene().getWindow().hide();
  }

  private void updateEstimate() {
    if (!hasPositiveQuantity()) {
      estimatedCostLabel.setText("Estimated cost: $0.00");
      return;
    }
    double unitPrice = currentUnitPrice();
    estimatedCostLabel.setText(
        "Estimated cost: " + validator.formatEstimatedCost(quantityField.getText(), unitPrice));
  }

  private boolean hasPositiveQuantity() {
    return validator.validate("MARKET", quantityField.getText(), "").isValid();
  }

  private double currentUnitPrice() {
    if ("LIMIT".equals(orderTypeComboBox.getValue())) {
      try {
        double limitPrice = Double.parseDouble(limitPriceField.getText().trim());
        if (Double.isFinite(limitPrice) && limitPrice > 0) {
          return limitPrice;
        }
      } catch (NumberFormatException exception) {
        return marketPriceForSelectedSymbol();
      }
    }
    return marketPriceForSelectedSymbol();
  }

  private double marketPriceForSelectedSymbol() {
    return switch (symbolComboBox.getValue()) {
      case "MSFT" -> 415.20;
      case "TSLA" -> 175.80;
      default -> 182.50;
    };
  }
}
