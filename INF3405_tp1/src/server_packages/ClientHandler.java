import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.*;
import java.nio.file.*;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;

public class ClientHandler extends Thread { 
    private Socket socket;
    private int clientNumber;
    private Path currentDirectory;
    private DataOutputStream out; 
    private DataInputStream in;  

    public ClientHandler(Socket socket, int clientNumber) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        this.currentDirectory = Paths.get(System.getProperty("user.dir"));
        System.out.println("New connection with client#" + clientNumber + " at " + socket);
    }
    
    private String[] command(String input){
        String[] command = input.split(" ", 2);
        
        if (command.length < 2) {
            command = new String[]{command[0], ""};
        }
        
        return command;
    }
   
    private void handleCommand(String[] command) throws IOException {
    	
    	switch(command[0])
    	{
    	case "ls" :
    		ls();
    		break; 
    		
    	case "mkdir":
    		mkdir(command[1]);
		    break; 
		    
    	case "cd":
    		cd(command[1]);
		    break;
		    
    	case "upload":
    		saveFile(command[1]);
		    break;
		    
    	case "download":
			if (isFileExist(command[1])) {    					
				sendFile(command[1]);
			}
		    break;
		    
    	case "delete":	
			delete(command[1]);
		    break;
		    
	    default:
	    	out.writeUTF("Unknown command: " + command[0]);
	    	System.out.println("Unknown command: " + command[0]);
	    	break;
    	}
    } 
    
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
    
    private void cd(String directoryName) throws IOException {
        try {
            if (directoryName.equals("..")) {
                currentDirectory = currentDirectory.getParent();
            } else {
                Path newDir = currentDirectory.resolve(directoryName);
                if (Files.isDirectory(newDir)) {
                    currentDirectory = newDir;
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
