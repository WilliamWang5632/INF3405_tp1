import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.regex.Pattern;

public class Server {
	
    private static final Pattern IP_PATTERN = 
		Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"); // https://stackoverflow.com/questions/5667371/validate-ipv4-address-in-java

    public static boolean isValidIP(final String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }
	
	public static boolean isValidPort(final int port) {
		if (port >= 5000 && port <= 5500){
			return true;
		}
		else {
			return false;
		}
	}
	
    private static ServerSocket Listener; 

    public static void main(String[] args) {
        int clientNumber = 0; 
        
        System.out.println("Veuillez entrez l'addresse IP de votre serveur: ");
        String serverAddress = System.console().readLine();
        while (!Server.isValidIP(serverAddress)){
            System.out.println("Adresse IP invalid! Veuillez saisir une valeur de forme x.x.x.x avec  une taille de 1 octet: ");
        	serverAddress = System.console().readLine();
        }
        
        System.out.println("Veuillez entrez l'addresse du port de votre serveur: ");
        int serverPort = Integer.parseInt(System.console().readLine());
        while (!Server.isValidPort(serverPort)){
            System.out.println("Port invalid! Veuillez saisir une valeur entre 5000 et 5500: ");
            serverPort = Integer.parseInt(System.console().readLine());
        }
        
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
