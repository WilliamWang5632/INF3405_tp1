import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Client class that connects to a server and handles file upload and download requests
 * over a socket. It allows users to interact with the server using commands.
 */

public class Client {
	
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    
    private static final Pattern IP_PATTERN = 
		Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"); 
    	//Using https://stackoverflow.com/questions/5667371/validate-ipv4-address-in-java
    
    /**
     * Verifies if the given IP address is valid based on the IPv4 format.
     *
     * @param ip The IP address to check.
     * @return true if the IP is valid, false otherwise.
     */
    
    public static boolean isValidIP(final String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }
    
    /**
     * Verifies if the port number is within the valid range (5000-5500).
     *
     * @param port The port number to check.
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
	
    /**
     * Splits the user input into a command and an argument.
     *
     * @param input The input string from the user.
     * @return A string array with the command and the argument.
     */
	
    private String[] command(String input){
        String[] command = input.split(" ", 2);
        
        if (command.length < 2) {
            command = new String[]{command[0], ""};
        }
        
        return command;
    }
    
    /**
     * Checks if the given file exists and is a regular file.
     *
     * @param fileName The name of the file to check.
     * @return true if the file exists and is a regular file, false otherwise.
     */
    
    private boolean isFileExist(String fileName) {
        Path filePath = Paths.get(fileName);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            System.out.println("File doesn't exist");
            return false;
        }
        return true;
    }
    
    /**
     * Processes the user command and sends the appropriate request to the server.
     * Supports "upload", "download"
     *
     * @param command The command array where the first element is the command and the second is the argument.
     */
    
    private void handleCommand(String[] command) throws IOException {
        switch(command[0]) {
            case "upload":
	        	if (isFileExist(command[1])){
	                out.writeUTF("upload " + command[1]); 
	                upload(new File(command[1]));
	        	} else {
	                return;
	        	}
                break;
                
            case "download":
                out.writeUTF("download " + command[1]); 
    	        String response = in.readUTF();
    	        if(response.equals("Sending file...")){    	        	
    	        	download(command[1]);
    	        } else {    	        	
    	        	System.out.println(response);
    	        }
                break;
                
            default:
                out.writeUTF(command[0] + (command[1].isEmpty() ? "" : " " + command[1]));
                out.flush();
                break;
        }
        catchResponse();  
    }

    /**
     * Processes the user command and sends the appropriate request to the server.
     * Supports "upload", "download", and other commands.
     *
     * @param command The command array where the first element is the command and the second is the argument.
     * @throws IOException If an I/O error occurs.
     */
	
    private void upload(File file) throws IOException { // Using https://stackoverflow.com/questions/10367698/java-multiple-file-transfer-over-socket
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		FileInputStream fis = new FileInputStream(file.toString());
		byte[] buffer = new byte[4096];
		int read;
		dos.writeLong(file.length());
		while ((read=fis.read(buffer)) > 0) {
			dos.write(buffer, 0, read);
		}
		
		fis.close();
    }
    
    /**
     * Downloads a file from the server and saves it locally with the specified name.
     *
     * @param fileName The name of the file to download.
     * @throws IOException If an I/O error occurs during the file download.
     */

    private void download(String fileName) throws IOException { 
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		FileOutputStream fos = new FileOutputStream(fileName);
		byte[] buffer = new byte[4096];
		long fileSize = dis.readLong();
		int read = 0;
		while(fileSize > 0 && (read = dis.read(buffer)) > 0) {
			fos.write(buffer, 0, read);
			fileSize -= read;
		}
		
		fos.close();
    }
    
    /**
     * Listens for responses from the server and prints them to the console
     * until the "Process done" message is received.
     */
    
	private void catchResponse(){
        String response = "";
	    try {
	        while (!(response = in.readUTF()).equals("Process done")) {
	            System.out.println(response);
	        }
	    } catch (IOException e) {
	        System.err.println("Error: " + e.getMessage());
	    }
	}
	
    /**
     * The main method where the client prompts the user for the server's IP address and port,
     * establishes a connection, and processes user commands.
     *
     * @param args Command-line arguments (not used).
     */

    public static void main(String[] args) {
    	Client client = new Client();
    	
        // Input the server's IP address

        System.out.println("Enter the IP address of your server:  ");
        String serverAddress = System.console().readLine();
        while (!Server.isValidIP(serverAddress)){
            System.out.println("Invalid IP address! Please enter a value in the form x.x.x.x with a size of 1 byte: ");
        	serverAddress = System.console().readLine();
        }
        
        // Input the server's port

        System.out.println("Enter the port address of your server: ");
        int serverPort = Integer.parseInt(System.console().readLine());
        while (!Server.isValidPort(serverPort)){
            System.out.println("Invalid port! Please enter a value between 5000 and 5500: ");
            serverPort = Integer.parseInt(System.console().readLine());
        }
        
        // Establish a connection with the server

        try {
            client.socket = new Socket(serverAddress, serverPort);
            client.out = new DataOutputStream(client.socket.getOutputStream());
            client.in = new DataInputStream(client.socket.getInputStream());
           
            System.out.format("Connected to the server [%s:%d]%n", serverAddress, serverPort);

            // Receive and print the server's welcome message

            String helloMessageFromServer = client.in.readUTF();
            System.out.println("Message from server: " + helloMessageFromServer);
            
            // Main command loop

            String command[];
            while (true) {
    	        System.out.println("Enter your command: ");
    	        command = client.command(System.console().readLine());
    	        if (command[0].equals("exit")) {
    	        	System.out.println("Disconnected");
    	        	break;
    	        } else {
    	        	client.handleCommand(command);
    	        }
            }
    	        

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                if (client.socket != null && !client.socket.isClosed()) {
                	client.socket.close();
                }
            } catch (IOException e) {
                System.err.println("Couldn't close the socket: " + e.getMessage());
            }
        }
    }
}
