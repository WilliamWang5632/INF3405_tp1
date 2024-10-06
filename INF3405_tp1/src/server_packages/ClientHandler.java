package server_packages;


import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends Thread { // pour traiter la demande de chaque client sur un socket particulier
    private Socket socket;
    private int clientNumber;

    public ClientHandler(Socket socket, int clientNumber) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        System.out.println("New connection with client#" + clientNumber + " at " + socket);
    }

    public void run() {
        try {
            // Création du canal d'envoi vers le client
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("Hello from server - you are client#" + clientNumber); // Envoi du message au client

        } catch (IOException e) {
            System.err.println("Error handling client# " + clientNumber + ": " + e.getMessage());

        } finally {
            try {
                // Fermeture de la connexion avec le client
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Couldn't close the socket: " + e.getMessage());
            }
            System.out.println("Connection with client# " + clientNumber + " closed");
        }
    }
}
