package org.serverchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean done;

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        try {
            connectToServer();
            startInputHandler();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            shutDown();
        }
    }

    private void connectToServer() throws IOException {
        client = new Socket("127.0.0.1", 9999);
        out = new PrintWriter(client.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    }

    private void startInputHandler() {
        InputHandler inputHandler = new InputHandler();
        Thread inputHandlerThread = new Thread(inputHandler);
        inputHandlerThread.start();
    }

    public void shutDown() {
        done = true;
        try {
            closeResources();
        } catch (IOException e) {
            // log or handle exception
        }
    }

    private void closeResources() throws IOException {
        in.close();
        out.close();
        if (!client.isClosed()) {
            client.close();
        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try (BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in))) {
                while (!done) {
                    String message = inReader.readLine();
                    if (message.equals("/quit")) {
                        out.println(message);
                        shutDown();
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                shutDown();
            }
        }
    }
}