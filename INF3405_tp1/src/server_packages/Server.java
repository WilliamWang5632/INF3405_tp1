import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.regex.Pattern;

/**
 * The Server class sets up a server that listens for client connections.
 * It validates IP addresses and ports, binds to a specific IP address and port, 
 * and accepts client connections to handle them in separate threads.
 */

public class Server {
	
    // Regular expression pattern to validate IPv4 addresses
    private static final Pattern IP_PATTERN = 
		Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"); // https://stackoverflow.com/questions/5667371/validate-ipv4-address-in-java
    
    /**
     * Checks if the given IP address is valid based on the IPv4 format.
     *
     * @param ip The IP address to validate.
     * @return true if the IP address is valid, false otherwise.
     */
    
    public static boolean isValidIP(final String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }
    
    /**
     * Checks if the port number is valid (between 5000 and 5500).
     *
     * @param port The port number to validate.
     * @return true if the port is valid, false otherwise.
     */
    
	public static boolean isValidPort(final int port) {
		if (port >= 5000 && port <= 5500){
			return true;
		}
		else {
			return false;
		}
	}
	
    // ServerSocket to listen for client connections

    private static ServerSocket Listener; 
    
    /**
     * The main method starts the server, binds it to a given IP address and port,
     * and listens for client connections in an infinite loop. Each client connection
     * is handled in a separate thread.
     *
     * @param args Command line arguments (not used).
     */
    
    public static void main(String[] args) {
        int clientNumber = 0; // Tracks the number of connected clients
        
        // Ask the user to input the server IP address

        System.out.println("Enter the IP address of your server:  ");
        String serverAddress = System.console().readLine();
        while (!Server.isValidIP(serverAddress)){
            System.out.println("Invalid IP address! Please enter a value in the form x.x.x.x with a size of 1 byte: ");
        	serverAddress = System.console().readLine();
        }
        
        // Ask the user to input the port number
        
        System.out.println("Enter the port address of your server: ");
        int serverPort = Integer.parseInt(System.console().readLine());
        while (!Server.isValidPort(serverPort)){
            System.out.println("Invalid port! Please enter a value between 5000 and 5500: ");
            serverPort = Integer.parseInt(System.console().readLine());
        }
        
        // Try to start the server and bind to the given IP and port
        
        try {
            Listener = new ServerSocket();
            Listener.setReuseAddress(true);
            InetAddress serverIP = InetAddress.getByName(serverAddress); // Convert IP string to InetAddress
            Listener.bind(new InetSocketAddress(serverIP, serverPort)); // Bind the server to the IP and port

            System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);
            
            // Continuously listen for client connections

            while (true) {
                new ClientHandler(Listener.accept(), clientNumber++).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            // Close the server socket when done
            try {
                if (Listener != null && !Listener.isClosed()) {
                    Listener.close();
                }
            } catch (IOException e) {
                System.err.println("Couldn't close the server socket: " + e.getMessage());
            }
        }
    }
}
