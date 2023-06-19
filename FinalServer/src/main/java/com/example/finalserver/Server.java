package com.example.finalserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int SERVER_PORT = 5570;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/db";
    private static final String DB_USERNAME = "username";
    private static final String DB_PASSWORD = "password";

    private ServerSocket serverSocket;
    private Connection dbConnection;
    private List<ClientHandler> clients;

    public static void main(String[] args) {
        Server chatServer = new Server();
        chatServer.start();
    }

    public void start() {
        System.out.println("Chat server started.");

        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            dbConnection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            clients = new ArrayList<>();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private BufferedWriter writer;
        private String currentUsername;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received message from client: " + message);

                    if (message.startsWith("SIGNUP")) {
                        handleSignup(message);
                    } else if (message.startsWith("LOGIN")) {
                        handleLogin(message);
                    } else {
                        sendMessageToAllClients(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnectClient();
            }
        }

        private void handleSignup(String message) {
            String[] parts = message.split(" ");
            if (parts.length == 3) {
                String username = parts[1];
                String password = parts[2];
                if (!userExists(username)) {
                    if (createUser(username, password)) {
                        sendMessageToClient("SIGNUP_SUCCESS");
                    } else {
                        sendMessageToClient("SIGNUP_FAILED");
                    }
                } else {
                    sendMessageToClient("USERNAME_EXISTS");
                }
            } else {
                sendMessageToClient("INVALID_COMMAND");
            }
        }

        private void handleLogin(String message) {
            String[] parts = message.split(" ");
            if (parts.length == 3) {
                String username = parts[1];
                String password = parts[2];
                if (authenticateUser(username, password)) {
                    sendMessageToClient("LOGIN_SUCCESS");
                    currentUsername = username;
                    sendMessageToAllClients("USER_JOINED " + username);
                } else {
                    sendMessageToClient("LOGIN_FAILED");
                }
            } else {
                sendMessageToClient("INVALID_COMMAND");
            }
        }

        private boolean userExists(String username) {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = dbConnection.prepareStatement("SELECT * FROM users WHERE username = ?");
                statement.setString(1, username);
                resultSet = statement.executeQuery();
                return resultSet.next();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                closeResultSet(resultSet);
                closeStatement(statement);
            }
            return false;
        }

        private boolean createUser(String username, String password) {
            PreparedStatement statement = null;
            try {
                statement = dbConnection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
                statement.setString(1, username);
                statement.setString(2, password);
                int rowsInserted = statement.executeUpdate();
                return rowsInserted > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                closeStatement(statement);
            }
            return false;
        }

        private boolean authenticateUser(String username, String password) {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = dbConnection.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
                statement.setString(1, username);
                statement.setString(2, password);
                resultSet = statement.executeQuery();
                return resultSet.next();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                closeResultSet(resultSet);
                closeStatement(statement);
            }
            return false;
        }

        private void sendMessageToClient(String message) {
            try {
                writer.write(message + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMessageToAllClients(String message) {
            for (ClientHandler client : clients) {
                if (!client.equals(this)) {
                    client.sendMessageToClient(message);
                }
            }
        }

        private void disconnectClient() {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                System.out.println("Client disconnected: " + clientSocket);

                if (currentUsername != null) {
                    sendMessageToAllClients("USER_LEFT " + currentUsername);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeWriter(writer);
                closeReader(reader);
            }
        }

        private void closeResultSet(ResultSet resultSet) {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeStatement(PreparedStatement statement) {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeReader(BufferedReader reader) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeWriter(BufferedWriter writer) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
