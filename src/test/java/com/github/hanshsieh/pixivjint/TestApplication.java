package com.github.hanshsieh.pixivjint;

import javafx.application.Application;
import javafx.stage.Stage;
public class TestApplication extends Application {
  private static Thread appThread = null;
  @Override
  public void start(Stage primaryStage) {
  }

  public static synchronized void launchIfNeeded() {
    if (appThread != null) {
      return;
    }
    appThread = new Thread(() -> launch(TestApplication.class));
    appThread.start();
  }
}