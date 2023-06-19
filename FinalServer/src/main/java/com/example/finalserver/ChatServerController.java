package com.example.finalserver;

import javafx.application.Platform;
import javafx.scene.control.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatServerController {
    private static final int SERVER_PORT = 5570;

    private ServerSocket serverSocket;
    private Map<String, BufferedWriter> connectedClients;
    private Set<String> busyClients;

    private TextArea logArea;
    private ListView<String> clientListView;
    private TextField usernameTextField;

    public ChatServerController(TextArea logArea, ListView<String> clientListView, TextField usernameTextField) {
        this.logArea = logArea;
        this.clientListView = clientListView;
        this.usernameTextField = usernameTextField;
        connectedClients = new HashMap<>();
        busyClients = new HashSet<>();
    }

    public void startServer() {
        if (serverSocket == null || serverSocket.isClosed()) {
            Thread serverThread = new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(SERVER_PORT);
                    log("Server started on port " + SERVER_PORT);

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    }
                } catch (IOException e) {
                    log("Error in server: " + e.getMessage());
                }
            });
            serverThread.start();
            log("Server started.");
        } else {
            log("Server is already running.");
        }
    }

    public void stopServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                log("Server stopped.");
            } catch (IOException e) {
                log("Error stopping server: " + e.getMessage());
            }
        } else {
            log("Server is not running.");
        }
    }

    public void kickClient() {
        String selectedClient = clientListView.getSelectionModel().getSelectedItem();
        if (selectedClient != null) {
            BufferedWriter writer = connectedClients.get(selectedClient);
            try {
                writer.write("You have been kicked from the chat room.\n");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log("Error kicking client: " + e.getMessage());
            }

            connectedClients.remove(selectedClient);
            busyClients.remove(selectedClient);
            log("Kicked client: " + selectedClient);

            // Update the client list view
            Platform.runLater(() -> clientListView.getItems().remove(selectedClient));
        }
    }

    public void addClient() {
        String username = usernameTextField.getText().trim();
        if (!username.isEmpty() && !connectedClients.containsKey(username)) {
            connectedClients.put(username, null);
            log("Added client: " + username);

            // Update the client list view
            Platform.runLater(() -> clientListView.getItems().add(username));

            usernameTextField.clear();
        }
    }

    public void removeClient() {
        String selectedClient = clientListView.getSelectionModel().getSelectedItem();
        if (selectedClient != null) {
            connectedClients.remove(selectedClient);
            busyClients.remove(selectedClient);
            log("Removed client: " + selectedClient);

            // Update the client list view
            Platform.runLater(() -> clientListView.getItems().remove(selectedClient));
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            // Read the username from the client
            String username = reader.readLine();
            log("Client connected: " + username);

            // Send a welcome message to the client
            writer.write("Welcome to the chat room, " + username + "!\n");
            writer.flush();

            // Add the client to the connected clients map
            connectedClients.put(username, writer);

            // Update the client list view
            Platform.runLater(() -> clientListView.getItems().add(username));

            // Start a new thread for handling client messages
            Thread clientThread = new Thread(() -> handleClientMessages(username, reader));
            clientThread.start();
        } catch (IOException e) {
            log("Error in client connection: " + e.getMessage());
        }
    }

    private void handleClientMessages(String username, BufferedReader reader) {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                log("Message from " + username + ": " + message);

                // Check if the client wants to change their availability
                if (message.equalsIgnoreCase("busy")) {
                    busyClients.add(username);
                } else if (message.equalsIgnoreCase("available")) {
                    busyClients.remove(username);
                } else {
                    // Broadcast the message to all connected clients
                    broadcastMessage(username + ": " + message);
                }
            }
        } catch (IOException e) {
            log("Error in client connection: " + e.getMessage());
        } finally {
            // Remove the client from the connected clients map and close the connection
            connectedClients.remove(username);
            busyClients.remove(username);
            log("Client disconnected: " + username);
            broadcastMessage(username + " has left the chat room.");

            // Update the client list view
            Platform.runLater(() -> clientListView.getItems().remove(username));
        }
    }

    private void broadcastMessage(String message) {
        for (BufferedWriter writer : connectedClients.values()) {
            try {
                writer.write(message + "\n");
                writer.flush();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }
}
