package org.serverchat;

import java.io.IOException;
import java.net.ServerSocket;

public class App {
    public static void main(String[] args) {
        try{
            ServerSocket serverSocket = new ServerSocket(1234);
            Server server = new Server(serverSocket);
            server.startServer();
        }catch (IOException e){
            System.out.println("Error!");
        }
    }
}
