package com.example.finalclient;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client extends Application {
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

    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/db";
    private static final String DB_USERNAME = "username";
    private static final String DB_PASSWORD = "password";

    private Connection dbConnection;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Client");

        // Initialize user credentials and chat log
        userCredentials = new HashMap<>();
        chatLog = new ArrayList<>();

        // Establish database connection
        try {
            dbConnection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (SQLException e) {
            showErrorAlert("Failed to connect to the database.");
            return;
        }

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

            if (username.isEmpty() || password.isEmpty()) {
                showErrorAlert("Username and password cannot be empty.");
                return;
            }

            if (userExists(username)) {
                showErrorAlert("Username already exists.");
                return;
            }

            if (createUser(username, password)) {
                currentUsername = username;
                scene.setRoot(chatRoomPane);
                primaryStage.setTitle("Chat Client - Chat Room");
                connectToServer();
            } else {
                showErrorAlert("Failed to create user.");
            }
        });

        // Create login button action
        Button loginButton = new Button("Login");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> {
            String username = usernameTextField.getText();
            String password = passwordTextField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showErrorAlert("Username and password cannot be empty.");
                return;
            }

            if (authenticateUser(username, password)) {
                currentUsername = username;
                scene.setRoot(chatRoomPane);
                primaryStage.setTitle("Chat Client - Chat Room");
                connectToServer();
            } else {
                showErrorAlert("Invalid username or password.");
            }
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

        chatRoomPane.setTop(chatRoomControlsContainer);
    }

    private VBox createLoginView() {
        VBox loginVBox = new VBox();
        loginVBox.setAlignment(Pos.CENTER);
        loginVBox.setSpacing(20);
        return loginVBox;
    }

    private VBox createSignupView() {
        VBox signupVBox = new VBox();
        signupVBox.setAlignment(Pos.CENTER);
        signupVBox.setSpacing(20);
        return signupVBox;
    }

    private boolean userExists(String username) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM users WHERE username = ?");
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean createUser(String username, String password) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean authenticateUser(String username, String password) {
        try {
            PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            showErrorAlert("Failed to connect to the server.");
        }
    }

    private void disconnectFromServer() {
        try {
            if (socket != null)
                socket.close();
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                chatTextArea.appendText(message + "\n");
                chatLog.add(message);
            }
        } catch (IOException e) {
            showErrorAlert("Failed to receive messages from the server.");
        }
    }

    private void sendMessage() {
        String message = messageTextField.getText();
        if (!message.isEmpty()) {
            try {
                writer.write(message + "\n");
                writer.flush();
                messageTextField.clear();
                chatLog.add(currentUsername + ": " + message);
            } catch (IOException e) {
                showErrorAlert("Failed to send message to the server.");
            }
        }
    }

    private void saveChatLog() {
        File file = new File("chat_log.txt");
        try (PrintWriter writer = new PrintWriter(file)) {
            for (String message : chatLog) {
                writer.println(message);
            }
            writer.flush();
            showAlert(Alert.AlertType.INFORMATION, "Chat Log Saved", "The chat log has been saved to " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            showErrorAlert("Failed to save chat log.");
        }
    }

    private void showErrorAlert(String message) {
        showAlert(Alert.AlertType.ERROR, "Error", message);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
