package com.example.finalclient;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatClient extends Application {
    private TextArea chatTextArea;
    private TextField messageTextField;
    private TextField usernameTextField;
    private PasswordField passwordTextField;
    private String currentUsername;
    private boolean isBusy;
    private Map<String, String> userCredentials; // User credentials (username and password)
    private List<String> chatLog; // Chat log
    private ComboBox<String> availabilityComboBox;

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5570;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Client");

        // Initialize user credentials and chat log
        userCredentials = new HashMap<>();
        chatLog = new ArrayList<>();

        // Create login and signup views
        VBox loginVBox = createLoginView();
        VBox signupVBox = createSignupView();

        // Create chat room view
        BorderPane chatRoomPane = new BorderPane();
        chatRoomPane.setPadding(new Insets(10));
        chatRoomPane.setPrefSize(400, 300);

        chatTextArea = new TextArea();
        chatTextArea.setEditable(false);

        messageTextField = new TextField();
        messageTextField.setOnAction(e -> sendMessage());

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        HBox messageBox = new HBox(10, messageTextField, sendButton);
        messageBox.setAlignment(Pos.CENTER);

        chatRoomPane.setCenter(chatTextArea);
        chatRoomPane.setBottom(messageBox);

        // Create main scene
        Scene scene = new Scene(loginVBox, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Switch to signup view on button click
        Button switchToSignupButton = new Button("Sign Up");
        switchToSignupButton.setOnAction(e -> {
            scene.setRoot(signupVBox);
            primaryStage.setTitle("Chat Client - Sign Up");
        });

        // Switch to login view on button click
        Button switchToLoginButton = new Button("Back to Login");
        switchToLoginButton.setOnAction(e -> {
            scene.setRoot(loginVBox);
            primaryStage.setTitle("Chat Client - Login");
        });

        // Create signup button action
        Button signupButton = new Button("Sign Up");
        signupButton.setDefaultButton(true);
        signupButton.setOnAction(e -> {
            String username = usernameTextField.getText();
            String password = passwordTextField.getText();

            if (userCredentials.containsKey(username)) {
                showErrorAlert("Username already exists");
                return;
            }

            userCredentials.put(username, password);
            currentUsername = username;
            scene.setRoot(chatRoomPane);
            primaryStage.setTitle("Chat Client - Chat Room");
            connectToServer();
        });

        // Create login button action
        Button loginButton = new Button("Login");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> {
            String username = usernameTextField.getText();
            String password = passwordTextField.getText();

            if (!userCredentials.containsKey(username) || !userCredentials.get(username).equals(password)) {
                showErrorAlert("Invalid username or password");
                return;
            }

            currentUsername = username;
            scene.setRoot(chatRoomPane);
            primaryStage.setTitle("Chat Client - Chat Room");
            connectToServer();
        });

        // Create logout button action
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> {
            scene.setRoot(loginVBox);
            primaryStage.setTitle("Chat Client - Login");
            currentUsername = null;
            disconnectFromServer();
        });

        // Create "Save Chat Log" button
        Button saveChatLogButton = new Button("Save Chat Log");
        saveChatLogButton.setOnAction(e -> saveChatLog());

        // Create availability radio buttons
        RadioButton availableRadioButton = new RadioButton("Available");
        availableRadioButton.setSelected(true);
        availableRadioButton.setOnAction(e -> isBusy = false);

        RadioButton busyRadioButton = new RadioButton("Busy");
        busyRadioButton.setOnAction(e -> isBusy = true);

        ToggleGroup availabilityToggleGroup = new ToggleGroup();
        availableRadioButton.setToggleGroup(availabilityToggleGroup);
        busyRadioButton.setToggleGroup(availabilityToggleGroup);

        HBox availabilityBox = new HBox(10, availableRadioButton, busyRadioButton);
        availabilityBox.setAlignment(Pos.CENTER_RIGHT);

        // Create text fields and password field
        usernameTextField = new TextField();
        passwordTextField = new PasswordField();

        // Create the layout for the login view
        VBox.setMargin(usernameTextField, new Insets(10));
        VBox.setMargin(passwordTextField, new Insets(10));
        VBox.setMargin(loginButton, new Insets(10));
        VBox.setMargin(switchToSignupButton, new Insets(10));

        VBox loginBox = new VBox(10, usernameTextField, passwordTextField, loginButton, switchToSignupButton);
        loginBox.setAlignment(Pos.CENTER);

        loginVBox.getChildren().addAll(loginBox);

        // Create the layout for the signup view
        VBox.setMargin(usernameTextField, new Insets(10));
        VBox.setMargin(passwordTextField, new Insets(10));
        VBox.setMargin(signupButton, new Insets(10));
        VBox.setMargin(switchToLoginButton, new Insets(10));

        VBox signupBox = new VBox(10, usernameTextField, passwordTextField, signupButton, switchToLoginButton);
        signupBox.setAlignment(Pos.CENTER);

        signupVBox.getChildren().addAll(signupBox);

        // Create the layout for the chat room view
        VBox.setVgrow(chatTextArea, Priority.ALWAYS);

        VBox chatRoomBox = new VBox(10, chatTextArea);
        chatRoomBox.setAlignment(Pos.CENTER);

        VBox chatRoomButtons = new VBox(10, saveChatLogButton, logoutButton);
        chatRoomButtons.setAlignment(Pos.CENTER_RIGHT);

        HBox chatRoomControls = new HBox(10, chatRoomBox, chatRoomButtons);
        chatRoomControls.setPadding(new Insets(10));

        HBox.setHgrow(chatRoomBox, Priority.ALWAYS);

        VBox chatRoomControlsContainer = new VBox(10);
        chatRoomControlsContainer.getChildren().addAll(availabilityBox, chatRoomControls);

        VBox chatRoomContainer = new VBox(10, chatRoomControlsContainer);
        chatRoomContainer.setAlignment(Pos.CENTER);

        chatRoomPane.setCenter(chatRoomContainer);
    }

    private VBox createLoginView() {
        VBox loginVBox = new VBox(10);
        loginVBox.setAlignment(Pos.CENTER);
        return loginVBox;
    }

    private VBox createSignupView() {
        VBox signupVBox = new VBox(10);
        signupVBox.setAlignment(Pos.CENTER);
        return signupVBox;
    }

    private void sendMessage() {
        String message = messageTextField.getText();
        if (!message.isEmpty()) {
            String formattedMessage = "[" + currentUsername + "]: " + message;
            chatTextArea.appendText(formattedMessage + "\n");
            chatLog.add(formattedMessage);
            messageTextField.clear();

            // Send message to server
            sendMessageToServer(message);
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            // Start a new thread for receiving messages from the server
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

            // Notify the server about the client's username and availability
            sendMessageToServer(currentUsername);
            sendMessageToServer(isBusy ? "busy" : "available");
        } catch (IOException e) {
            showErrorAlert("Failed to connect to the server.");
        }
    }

    private void disconnectFromServer() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void sendMessageToServer(String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            showErrorAlert("Failed to send message to the server.");
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                // Process received messages from the server
                processMessageFromServer(message);
            }
        } catch (IOException e) {
            showErrorAlert("Connection to the server has been lost.");
            disconnectFromServer();
        }
    }

    private void processMessageFromServer(String message) {
        // Handle the received message from the server
        chatTextArea.appendText(message + "\n");
        chatLog.add(message);
    }

    private void saveChatLog() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String fileName = "chatlog_" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(fileName)) {
            for (String message : chatLog) {
                writer.println(message);
            }
            showInfoAlert("Chat log saved to file: " + fileName);
        } catch (FileNotFoundException e) {
            showErrorAlert("Failed to save chat log.");
        }
    }
}
