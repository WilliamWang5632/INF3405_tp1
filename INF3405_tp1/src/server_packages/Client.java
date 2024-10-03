package server_packages;


import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
    private static Socket socket;

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // Adresse du serveur
        int port = 5000; // Port du serveur

        try {
            // Création d'une nouvelle connexion avec le serveur
            socket = new Socket(serverAddress, port);
            System.out.format("Connected to the server [%s:%d]%n", serverAddress, port);

            // Création d'un canal entrant pour recevoir les messages du serveur
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Attente de la réception d'un message envoyé par le serveur
            String helloMessageFromServer = in.readUTF();
            System.out.println("Message from server: " + helloMessageFromServer);

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                // Fermeture de la connexion avec le serveur
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Couldn't close the socket: " + e.getMessage());
            }
        }
    }
}
