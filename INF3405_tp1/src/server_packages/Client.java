import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Client {
	
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
	
    private void upload(String fileName) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            Path targetFile = currentDirectory.resolve(fileName);
            
            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                byte[] buffer = new byte[4096]; 
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead); 
                }
            }
            System.out.println("Uploaded file: " + fileName + " to " + targetFile);
        } catch (IOException e) {
            System.err.println("Error uploading file: " + e.getMessage());
        }
    }
    
    private void download(String fileName) {
        try {
            Path fileToDownload = currentDirectory.resolve(fileName);
            if (Files.exists(fileToDownload) && Files.isRegularFile(fileToDownload)) {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream(fileToDownload.toFile());
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                fis.close();
                System.out.println("Downloaded file: " + fileName);
            } else {
                System.out.println("File does not exist: " + fileToDownload);
            }
        } catch (IOException e) {
            System.err.println("Error downloading file: " + e.getMessage());
        }
    }
    
    private static Socket socket;
    private Path currentDirectory = Paths.get(System.getProperty("user.dir"));

    public static void main(String[] args) {
    	
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
            // Création d'une nouvelle connexion avec le serveur
            socket = new Socket(serverAddress, serverPort);
            System.out.format("Connected to the server [%s:%d]%n", serverAddress, serverPort);

            // Création d'un canal entrant pour recevoir les messages du serveur
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Attente de la réception d'un message envoyé par le serveur
            String helloMessageFromServer = in.readUTF();
            System.out.println("Message from server: " + helloMessageFromServer);
            
            String command = "";
            while (!command.equals("exit")) {
    	        System.out.println("\nVeuillez entrez votre commande: ");
    	        command = System.console().readLine();
    	        
            
    	        

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
