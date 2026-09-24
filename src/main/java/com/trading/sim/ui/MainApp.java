package com.trading.sim.ui;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Launches the JavaFX paper-trading dashboard. */
public final class MainApp extends Application {

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/MainView.fxml"));
    Parent root = loader.load();
    Scene scene = new Scene(root, 1440, 900);
    scene.getStylesheets().add(
        MainApp.class.getResource("/css/dark-theme.css").toExternalForm());

    stage.setTitle("Paper Trading Simulator - Realtime System");
    stage.setMinWidth(1100);
    stage.setMinHeight(720);
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
