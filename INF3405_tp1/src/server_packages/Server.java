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
        
        System.out.println("Enter the IP address of your server:  ");
        String serverAddress = System.console().readLine();
        while (!Server.isValidIP(serverAddress)){
            System.out.println("Invalid IP address! Please enter a value in the form x.x.x.x with a size of 1 byte: ");
        	serverAddress = System.console().readLine();
        }
        
        System.out.println("Enter the port address of your server: ");
        int serverPort = Integer.parseInt(System.console().readLine());
        while (!Server.isValidPort(serverPort)){
            System.out.println("Invalid port! Please enter a value between 5000 and 5500: ");
            serverPort = Integer.parseInt(System.console().readLine());
        }
        
        try {
            Listener = new ServerSocket();
            Listener.setReuseAddress(true);
            InetAddress serverIP = InetAddress.getByName(serverAddress);
            Listener.bind(new InetSocketAddress(serverIP, serverPort));

            System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);

            while (true) {
                new ClientHandler(Listener.accept(), clientNumber++).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
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
