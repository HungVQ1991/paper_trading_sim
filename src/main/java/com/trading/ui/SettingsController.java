package com.trading.ui;

import com.trading.market.MarketProviderType;
import com.trading.market.MarketServiceProviderManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;

public class SettingsController {

  private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());
  private static final String CONFIG_FILE_PATH = "config.properties";

  @FXML
  private ComboBox<MarketProviderType> cmbProvider;

  @FXML
  private TextField txtTwelveDataKey;

  @FXML
  private TextField txtFinnhubKey;

  @FXML
  private Button btnApply;

  @FXML
  private Button btnResetAccount;

  @FXML
  private Label lblStatus;

  private MarketServiceProviderManager providerManager;
  private Runnable resetAccountAction;

  @FXML
  public void initialize() {
    if (cmbProvider != null) {
      cmbProvider.setItems(FXCollections.observableArrayList(MarketProviderType.values()));
      cmbProvider.setCellFactory(param -> new ListCell<>() {
        @Override
        protected void updateItem(MarketProviderType item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setText(null);
          } else {
            setText(item.getDisplayName());
          }
        }
      });
      cmbProvider.setButtonCell(new ListCell<>() {
        @Override
        protected void updateItem(MarketProviderType item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setText(null);
          } else {
            setText(item.getDisplayName());
          }
        }
      });
      cmbProvider.setValue(MarketProviderType.MOCK_ENGINE);
    }

    loadConfiguration();
  }

  public void setProviderManager(MarketServiceProviderManager providerManager) {
    this.providerManager = providerManager;
    if (providerManager != null && providerManager.getActiveProviderType() != null) {
      if (cmbProvider != null) {
        cmbProvider.setValue(providerManager.getActiveProviderType());
      }
    }
  }

  public void setResetAccountAction(Runnable resetAccountAction) {
    this.resetAccountAction = resetAccountAction;
  }

  @FXML
  public void handleApply() {
    if (cmbProvider == null) {
      return;
    }
    MarketProviderType selectedType = cmbProvider.getValue();
    if (selectedType == null) {
      updateStatus("Vui lòng chọn nguồn cấp dữ liệu!", true);
      return;
    }

    String apiKey = "";
    if (selectedType == MarketProviderType.TWELVE_DATA && txtTwelveDataKey != null) {
      apiKey = txtTwelveDataKey.getText().trim();
    } else if (selectedType == MarketProviderType.FINNHUB && txtFinnhubKey != null) {
      apiKey = txtFinnhubKey.getText().trim();
    }

    if (providerManager != null) {
      try {
        providerManager.switchProvider(selectedType, apiKey);
        updateStatus("Đã chuyển sang nguồn: " + selectedType.getDisplayName(), false);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error switching market provider", ex);
        updateStatus("Lỗi chuyển đổi: " + ex.getMessage(), true);
        return;
      }
    }

    saveConfiguration(selectedType);
  }

  @FXML
  public void handleResetAccount() {
    if (resetAccountAction != null) {
      try {
        resetAccountAction.run();
        updateStatus("Đã đặt lại số dư tài khoản về $100,000", false);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error resetting account", ex);
        updateStatus("Lỗi đặt lại tài khoản: " + ex.getMessage(), true);
      }
    } else {
      updateStatus("Tài khoản đã được đặt lại.", false);
    }
  }

  private void updateStatus(String message, boolean isError) {
    if (lblStatus != null) {
      lblStatus.setText(message);
      lblStatus.setStyle(isError ? "-fx-text-fill: #ef5350;" : "-fx-text-fill: #26a69a;");
    }
  }

  private void loadConfiguration() {
    Properties props = new Properties();
    Path path = Paths.get(CONFIG_FILE_PATH);
    if (Files.exists(path)) {
      try (InputStream in = Files.newInputStream(path)) {
        props.load(in);
      } catch (Exception ex) {
        LOGGER.log(Level.FINE, "Could not load local config.properties", ex);
      }
    } else {
      try (InputStream in = getClass().getResourceAsStream("/config/config.properties")) {
        if (in != null) {
          props.load(in);
        }
      } catch (Exception ex) {
        LOGGER.log(Level.FINE, "Could not load resource config.properties", ex);
      }
    }

    String defaultProvider = props.getProperty("market.provider.default", "MOCK_ENGINE");
    try {
      if (cmbProvider != null) {
        cmbProvider.setValue(MarketProviderType.valueOf(defaultProvider));
      }
    } catch (Exception ex) {
      LOGGER.log(Level.FINE, "Invalid default provider enum: " + defaultProvider, ex);
    }

    if (txtTwelveDataKey != null) {
      txtTwelveDataKey.setText(props.getProperty("market.provider.twelvedata.key", ""));
    }
    if (txtFinnhubKey != null) {
      txtFinnhubKey.setText(props.getProperty("market.provider.finnhub.key", ""));
    }
  }

  private void saveConfiguration(MarketProviderType selectedType) {
    Properties props = new Properties();
    props.setProperty("market.provider.default", selectedType.name());
    if (txtTwelveDataKey != null) {
      props.setProperty("market.provider.twelvedata.key", txtTwelveDataKey.getText().trim());
    }
    if (txtFinnhubKey != null) {
      props.setProperty("market.provider.finnhub.key", txtFinnhubKey.getText().trim());
    }
    props.setProperty("account.initial.cash", "100000.0");

    try (OutputStream out = Files.newOutputStream(Paths.get(CONFIG_FILE_PATH))) {
      props.store(out, "Paper Trading Simulation Configuration");
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Failed to persist configuration", ex);
    }
  }
}
