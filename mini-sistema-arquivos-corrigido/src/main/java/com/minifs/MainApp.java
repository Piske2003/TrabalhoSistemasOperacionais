package com.minifs;

import com.minifs.controller.FileSystemController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override public void start(Stage stage) {
        FileSystemController controller = new FileSystemController();
        Scene scene = new Scene(controller.getMainView(), 1050, 760);
        stage.setTitle("Mini Sistema de Arquivos");
        stage.setMinWidth(900); stage.setMinHeight(650);
        stage.setScene(scene); stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
