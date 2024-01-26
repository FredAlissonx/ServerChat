package org.serverchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private volatile boolean done;
    private ExecutorService pool;

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        try {
            initializeServer();
            startConnectionHandlers();
        } catch (Exception e) {
            shutDown();
        }
    }

    private void initializeServer() throws IOException {
        connections = new ArrayList<>();
        server = new ServerSocket(9999);
        pool = Executors.newCachedThreadPool();
    }

    private void startConnectionHandlers() {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(client);
                connections.add(connectionHandler);
                pool.execute(connectionHandler);
            } catch (IOException e) {
                shutDown();
            }
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutDown() {
        done = true;
        try {
            closeResources();
        } catch (IOException e) {
            // log or handle exception
        }
        connections.forEach(ConnectionHandler::shutDown);
    }

    private void closeResources() throws IOException {
        pool.shutdown();
        if (!server.isClosed()) {
            server.close();
        }
    }

    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickName;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                initializeStreams();
                processClientInput();
            } catch (IOException e) {
                shutDown();
            }
        }

        private void initializeStreams() throws IOException {
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out.println("Please enter a nickname: ");
            nickName = in.readLine();
            System.out.println(nickName + " connected!");
            broadcast(nickName + " joined the chat!");
        }

        private void processClientInput() throws IOException {
            String message;
            while ((message = in.readLine()) != null) {
                processMessage(message);
            }
        }

        private void processMessage(String message) {
            if (message.startsWith("/nick")) {
                handleNickCommand(message);
            } else if (message.startsWith("/quit")) {
                handleQuitCommand();
            } else {
                broadcast(nickName + ": " + message);
            }
        }

        private void handleNickCommand(String message) {
            String[] messageSplit = message.split(" ", 2);
            if (messageSplit.length == 2) {
                broadcast(nickName + " renamed themselves to " + messageSplit[1]);
                System.out.println(nickName + " renamed themselves to " + messageSplit[1]);
                nickName = messageSplit[1];
                out.println("Successfully changed nickname to " + nickName);
            } else {
                out.println("No nickname provided!");
            }
        }

        private void handleQuitCommand() {
            broadcast(nickName + " left the chat!");
            System.out.println(nickName + " left the chat!");
            shutDown();
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutDown() {
            try {
                closeStreams();
            } catch (IOException e) {
                // log or handle exception
            }
        }

        private void closeStreams() throws IOException {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        }
    }
}
