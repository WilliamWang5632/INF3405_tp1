import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.*;
import java.nio.file.*;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Handles client connections and manages file operations such as listing directories, 
 * creating directories, changing directories, uploading, downloading, and deleting files.
 */

public class ClientHandler extends Thread { 
    private Socket socket;
    private int clientNumber;
    private Path currentDirectory;
    private DataOutputStream out; 
    private DataInputStream in;  
    
    /**
     * Constructor for ClientHandler.
     * Initializes the client socket, client number, and sets the current working directory to the server's root directory.
     *
     * @param socket       The client socket.
     * @param clientNumber The number associated with the client.
     */

    public ClientHandler(Socket socket, int clientNumber) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        this.currentDirectory = Paths.get(System.getProperty("user.dir"));
        System.out.println("New connection with client#" + clientNumber + " at " + socket);
    }
    
    /**
     * Splits the input string from the client into command and argument.
     *
     * @param input The input string from the client.
     * @return A string array where the first element is the command and the second element is the argument.
     */
    
    private String[] command(String input){
        String[] command = input.split(" ", 2);
        
        if (command.length < 2) {
            command = new String[]{command[0], ""};
        }
        
        return command;
    }
   
    /**
     * Handles the client's command and executes the appropriate method for the given command.
     *
     * @param command The command and its argument from the client.
     * @throws IOException If an I/O error occurs.
     */
    
    private void handleCommand(String[] command) throws IOException {
    	
    	switch(command[0])
    	{
    	case "ls" :
    		ls(); // Lists files in the current directory
    		break; 
    		
    	case "mkdir":
    		mkdir(command[1]); // Creates a new directory
		    break; 
		    
    	case "cd":
    		cd(command[1]); // Changes the current directory
		    break;
		    
    	case "upload":
    		saveFile(command[1]); // Saves a file uploaded by the client
		    break;
		    
    	case "download":
			if (isFileExist(command[1])) {    					
				sendFile(command[1]); // Sends a file to the client
			}
		    break;
		    
    	case "delete":	
			delete(command[1]); // Deletes a file or directory
		    break;
		    
	    default:
	    	out.writeUTF("Unknown command: " + command[0]);
	    	System.out.println("Unknown command: " + command[0]);
	    	break;
    	}
    } 
    
    /**
     * Lists all files and directories in the current directory.
     * 
     * @throws IOException If an I/O error occurs during file listing.
     */
    
    private void ls() throws IOException {
        try {
        	
            DirectoryStream<Path> stream = Files.newDirectoryStream(currentDirectory);
            StringBuilder filesList = new StringBuilder("Files in " + currentDirectory + ":\n");

            for (Path entry : stream) {
                filesList.append(entry.getFileName()).append("\n");
            }
            stream.close();
            out.writeUTF(filesList.toString());

        } catch (IOException e) {
            out.writeUTF("Error listing files: " + e.getMessage());
            System.err.println("Error listing files: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new directory in the current directory.
     * 
     * @param directoryName The name of the directory to create.
     * @throws IOException If an error occurs while creating the directory.
     */
    
    private void mkdir(String directoryName) throws IOException {
        try {
            Path newDir = currentDirectory.resolve(directoryName);
            Files.createDirectory(newDir);
            out.writeUTF("Created directory: " + newDir);
        } catch (IOException e) {
            out.writeUTF("Error creating directory: " + e.getMessage());
            System.err.println("Error creating directory: " + e.getMessage());
        }
    }
    
    /**
     * Changes the current directory.
     * 
     * @param directoryName The name of the directory to change to.
     * @throws IOException If an error occurs while changing the directory.
     */
    
    private void cd(String directoryName) throws IOException {
        try {
            if (directoryName.equals("..")) {
                currentDirectory = currentDirectory.getParent();
            } else {
                Path newDir = currentDirectory.resolve(directoryName);
                if (Files.isDirectory(newDir)) {
                    currentDirectory = newDir; // Change to the new directory
                } else {
                    out.writeUTF("Directory does not exist: " + newDir);
                    return;
                }
            }
            out.writeUTF("Current directory changed to: " + currentDirectory);
        } catch (Exception e) {
            out.writeUTF("Error changing directory: " + e.getMessage());
            System.err.println("Error changing directory: " + e.getMessage());
        }
    }
    
    /**
     * Saves a file sent by the client.
     * 
     * @param fileName The name of the file to save.
     * @throws IOException If an error occurs during file upload.
     */
    
	private void saveFile(String fileName) throws IOException { // Using https://stackoverflow.com/questions/858980/file-to-byte-in-java
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
		out.writeUTF(fileName + " succesfully uploaded");
	}
	
    /**
     * Checks if a file exists in the current directory.
     * 
     * @param fileName The name of the file to check.
     * @return true if the file exists, false otherwise.
     * @throws IOException If an I/O error occurs.
     */

    private boolean isFileExist(String fileName) throws IOException {
        Path filePath = currentDirectory.resolve(fileName);
        if (Files.notExists(filePath)) {
            out.writeUTF("File does not exist.");
            return false;
        } else {
            out.writeUTF("Sending file...");
            return true;
        }
    }
    
    /**
     * Sends a file to the client.
     * 
     * @param fileName The name of the file to send.
     * @throws IOException If an error occurs during file download.
     */

	private void sendFile(String fileName) throws IOException { 
		
		File file = currentDirectory.resolve(fileName).toFile();
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		FileInputStream fis = new FileInputStream(file.toString());
		byte[] buffer = new byte[4096];
		int read;
		dos.writeLong(file.length());
		while ((read=fis.read(buffer)) > 0) {
			dos.write(buffer, 0, read);
		}
		fis.close();
		out.writeUTF(fileName + " succesfully downloaded");
	}

    /**
     * Deletes a file or directory in the current directory.
     * 
     * @param name The name of the file or directory to delete.
     * @throws IOException If an error occurs during deletion.
     */

    private void delete(String name) throws IOException {
        try {
            Path pathToDelete = currentDirectory.resolve(name);
            if (Files.exists(pathToDelete)) {
                Files.delete(pathToDelete);
                out.writeUTF("Deleted: " + pathToDelete);
            } else {
                out.writeUTF("File or directory does not exist: " + pathToDelete);
            }
        } catch (IOException e) {
            out.writeUTF("Error deleting file/directory: " + e.getMessage());
            System.err.println("Error deleting file/directory: " + e.getMessage());
        }
    }

    /**
     * The main logic for handling client commands. 
     * It reads commands from the client, processes them, and sends responses back.
     */

    public void run() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            out.writeUTF("Hello from server - you are client#" + clientNumber); 

            String clientCommand;
            String clientAddress = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();
            while (true) {
                clientCommand = in.readUTF();
                if (clientCommand.equals("exit")) {
                    System.out.println("Client requested exit."); 
                    break;
                }
                
                // Log the client command with timestamp
                
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");
                Date date = new Date();
                System.out.println("[" + clientAddress + ":" + clientPort + " - " + dateFormat.format(date) + "]" + " : " + clientCommand);
                
                String[] command = command(clientCommand);
                handleCommand(command);
                
                out.writeUTF("Process done");
            }
        } catch (IOException e) {
            System.err.println("Error handling client# " + clientNumber + ": " + e.getMessage());

        } finally {
            try {
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
