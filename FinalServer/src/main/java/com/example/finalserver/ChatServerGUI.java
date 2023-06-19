package com.example.finalserver;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatServerGUI extends Application {

    private TextArea logArea;
    private ListView<String> clientListView;
    private TextField usernameTextField;

    private ChatServerController serverController;

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Server");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);

        clientListView = new ListView<>();
        clientListView.setPrefHeight(200);

        usernameTextField = new TextField();
        usernameTextField.setPromptText("Username");

        Button startButton = new Button("Start Server");
        startButton.setOnAction(e -> serverController.startServer());

        Button stopButton = new Button("Stop Server");
        stopButton.setOnAction(e -> serverController.stopServer());

        Button kickButton = new Button("Kick Client");
        kickButton.setOnAction(e -> serverController.kickClient());

        Button addButton = new Button("Add Client");
        addButton.setOnAction(e -> serverController.addClient());

        Button removeButton = new Button("Remove Client");
        removeButton.setOnAction(e -> serverController.removeClient());

        HBox buttonBox = new HBox(startButton, stopButton, kickButton);
        buttonBox.setSpacing(10);

        VBox root = new VBox(logArea, clientListView, usernameTextField, addButton, removeButton, buttonBox);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> serverController.stopServer()); // Stop the server when the window is closed
        primaryStage.show();

        // Create an instance of the ChatServerController and pass the GUI elements
        serverController = new ChatServerController(logArea, clientListView, usernameTextField);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
