package main.java.model;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        //this.primaryStage = primaryStage;
       new UserLogin(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
