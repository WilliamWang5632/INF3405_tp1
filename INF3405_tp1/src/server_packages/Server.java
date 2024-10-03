package server_packages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class Server {
    private static ServerSocket Listener; // Application Serveur

    public static void main(String[] args) {
        int clientNumber = 0; // Compteur incrémenté à chaque connexion d'un client au serveur
        String serverAddress = "127.0.0.1"; // Adresse du serveur
        int serverPort = 5000; // Port du serveur
        
        try {
            // Création de la connexion pour communiquer avec les clients
            Listener = new ServerSocket();
            Listener.setReuseAddress(true);
            InetAddress serverIP = InetAddress.getByName(serverAddress);
            // Association de l'adresse et du port à la connexion
            Listener.bind(new InetSocketAddress(serverIP, serverPort));

            System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);

            while (true) {
                // La fonction accept() est bloquante, attend qu'un client se connecte
                new ClientHandler(Listener.accept(), clientNumber++).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            try {
                // Fermeture de la connexion
                if (Listener != null && !Listener.isClosed()) {
                    Listener.close();
                }
            } catch (IOException e) {
                System.err.println("Couldn't close the server socket: " + e.getMessage());
            }
        }
    }
}
