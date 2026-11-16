package com.trading.sim.ui;

import com.trading.engine.TradingEngine;
import com.trading.model.LimitOrder;
import com.trading.model.MarketOrder;
import com.trading.model.Order;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/** Handles order entry and delegates financial validation to the trading engine. */
public final class OrderDialogController {

  private final OrderFormValidator validator = new OrderFormValidator();
  private TradingEngine tradingEngine;
  private ToDoubleFunction<String> priceProvider;
  private Runnable orderChanged;

  @FXML
  private ComboBox<String> symbolComboBox;

  @FXML
  private ComboBox<String> orderTypeComboBox;

  @FXML
  private RadioButton buyRadio;

  @FXML
  private RadioButton sellRadio;

  @FXML
  private TextField quantityField;

  @FXML
  private TextField limitPriceField;

  @FXML
  private Label estimatedCostLabel;

  @FXML
  private Label validationMessageLabel;

  /** Supplies the engine, current prices, and lifecycle callbacks after FXML loading. */
  public void configure(
      TradingEngine engine,
      ToDoubleFunction<String> latestPriceProvider,
      Runnable onOrderChanged) {
    tradingEngine = engine;
    priceProvider = latestPriceProvider;
    orderChanged = onOrderChanged;
  }

  @FXML
  private void initialize() {
    symbolComboBox.getItems().setAll("AAPL", "MSFT", "TSLA");
    symbolComboBox.getSelectionModel().selectFirst();
    orderTypeComboBox.getItems().setAll("MARKET", "LIMIT");
    orderTypeComboBox.getSelectionModel().selectFirst();
    buyRadio.setSelected(true);
    quantityField.textProperty().addListener((observable, oldValue, newValue) -> updateEstimate());
    limitPriceField.textProperty().addListener((observable, oldValue, newValue) -> updateEstimate());
    symbolComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateEstimate());
    updateOrderType();
  }

  @FXML
  private void updateOrderType() {
    boolean limit = "LIMIT".equals(orderTypeComboBox.getValue());
    limitPriceField.setDisable(!limit);
    if (!limit) {
      limitPriceField.clear();
    }
    updateEstimate();
  }

  @FXML
  private void confirmOrder() {
    String orderType = orderTypeComboBox.getValue();
    OrderFormValidator.ValidationResult validation =
        validator.validate(orderType, quantityField.getText(), limitPriceField.getText());
    if (!validation.isValid()) {
      validationMessageLabel.setText(validation.message());
      return;
    }

    int quantity = Integer.parseInt(quantityField.getText().trim());
    boolean isBuy = buyRadio.isSelected();
    String symbol = symbolComboBox.getValue();
    double currentPrice = priceProvider.applyAsDouble(symbol);
    Order order;
    if ("LIMIT".equals(orderType)) {
      order = new LimitOrder(
          symbol, isBuy, quantity, Double.parseDouble(limitPriceField.getText().trim()));
    } else {
      order = new MarketOrder(symbol, isBuy, quantity);
    }

    if (currentPrice <= 0.0 || !tradingEngine.placeOrder(order, currentPrice)) {
      validationMessageLabel.setText(
          "Order rejected: check available cash, shares, and market price.");
      return;
    }
    orderChanged.run();
    closeDialog();
  }

  @FXML
  private void closeDialog() {
    Stage stage = (Stage) quantityField.getScene().getWindow();
    stage.close();
  }

  private void updateEstimate() {
    validationMessageLabel.setText("");
    if (!validator.validate("MARKET", quantityField.getText(), "").isValid()) {
      estimatedCostLabel.setText("Estimated cost: $0.00");
      return;
    }
    double unitPrice = currentUnitPrice();
    estimatedCostLabel.setText(
        "Estimated cost: " + validator.formatEstimatedCost(quantityField.getText(), unitPrice));
  }

  private double currentUnitPrice() {
    if ("LIMIT".equals(orderTypeComboBox.getValue())) {
      try {
        double limit = Double.parseDouble(limitPriceField.getText().trim());
        if (Double.isFinite(limit) && limit > 0.0) {
          return limit;
        }
      } catch (NumberFormatException exception) {
        return priceProvider == null ? 0.0 : priceProvider.applyAsDouble(symbolComboBox.getValue());
      }
    }
    return priceProvider == null ? 0.0 : priceProvider.applyAsDouble(symbolComboBox.getValue());
  }
}
