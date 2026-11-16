package com.trading.sim.ui;

import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Controls the offline provider settings dialog. */
public final class SettingsController {

  @FXML
  private ComboBox<String> providerComboBox;

  @FXML
  private TextField apiKeyField;

  @FXML
  private Label statusLabel;

  private Consumer<String> providerChanged;

  /** Configures the dialog with the active provider and update callback. */
  public void configure(String activeProvider, Consumer<String> onProviderChanged) {
    providerChanged = onProviderChanged;
    providerComboBox.getSelectionModel().select(activeProvider);
  }

  @FXML
  private void saveSettings() {
    String provider = providerComboBox.getValue();
    providerChanged.accept(provider);
    statusLabel.setText("Settings saved. Mock Demo remains active in this build.");
  }
}
