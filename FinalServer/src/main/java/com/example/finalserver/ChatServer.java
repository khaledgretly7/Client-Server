package com.example.finalserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatServer {
    private static final int SERVER_PORT = 5570;

    private ServerSocket serverSocket;
    private Map<String, BufferedWriter> connectedClients;
    private Set<String> busyClients;

    public ChatServer() {
        connectedClients = new HashMap<>();
        busyClients = new HashSet<>();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
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
            System.out.println("Client connected: " + username);

            // Send a welcome message to the client
            writer.write("Welcome to the chat room, " + username + "!\n");
            writer.flush();

            // Add the client to the connected clients map
            connectedClients.put(username, writer);

            // Start a new thread for handling client messages
            Thread clientThread = new Thread(() -> handleClientMessages(username, reader));
            clientThread.start();
        } catch (IOException e) {
            System.err.println("Error in client connection: " + e.getMessage());
        }
    }

    private void handleClientMessages(String username, BufferedReader reader) {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Message from " + username + ": " + message);

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
            System.err.println("Error in client connection: " + e.getMessage());
        } finally {
            // Remove the client from the connected clients map and close the connection
            connectedClients.remove(username);
            busyClients.remove(username);
            System.out.println("Client disconnected: " + username);
            broadcastMessage(username + " has left the chat room.");
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

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}
