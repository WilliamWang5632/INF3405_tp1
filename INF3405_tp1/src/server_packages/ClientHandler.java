import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import java.io.*;
import java.nio.file.*;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;

public class ClientHandler extends Thread { // pour traiter la demande de chaque client sur un socket particulier
    private Socket socket;
    private int clientNumber;
    private Path currentDirectory;

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
	    	System.out.println("Unknown command: " + command[0]);
	    	break;
    	}
    } 
    
    private void ls() {
        try {
        	
            DirectoryStream<Path> stream = Files.newDirectoryStream(currentDirectory);
            StringBuilder filesList = new StringBuilder("Files in " + currentDirectory + ":\n");

            for (Path entry : stream) {
                filesList.append(entry.getFileName()).append("\n");
            }
            stream.close();
            System.out.println(filesList.toString());

        } catch (IOException e) {
            System.err.println("Error listing files: " + e.getMessage());
        }
    }
    
    private void mkdir(String directoryName) {
        try {
            Path newDir = currentDirectory.resolve(directoryName);
            Files.createDirectory(newDir);
            System.out.println("Created directory: " + newDir);
        } catch (IOException e) {
            System.err.println("Error creating directory: " + e.getMessage());
        }
    }
    
    private void cd(String directoryName) {
        try {
            if (directoryName.equals("..")) {
                currentDirectory = currentDirectory.getParent();
            } else {
                Path newDir = currentDirectory.resolve(directoryName);
                if (Files.isDirectory(newDir)) {
                    currentDirectory = newDir;
                } else {
                    System.out.println("Directory does not exist: " + newDir);
                }
            }
            System.out.println("Current directory changed to: " + currentDirectory);
        } catch (Exception e) {
            System.err.println("Error changing directory: " + e.getMessage());
        }
    }
    
    private void saveFile(String fileName) throws IOException {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                FileOutputStream fos = new FileOutputStream(fileName)) {
            long fileSize = dis.readLong();
            byte[] buffer = new byte[4096];
            int read;
            while (fileSize > 0 && (read = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
                fileSize -= read;
            }
            fos.close();
            System.out.println("Le fichier " + fileName + " a ete enregistre");
        } catch (IOException e) {
        	System.out.println("Erreur lors du enregistrement du fichier: " + e.getMessage());
        }
    }

    private boolean isFileExist(String fileName) {
        Path filePath = currentDirectory.resolve(fileName);
        if (Files.notExists(filePath)) {
            System.out.println("Ce fichier n'existe pas.");
            return false;
        } else {
        	System.out.println("Downloading...");
            return true;
        }
    }

    private void sendFile(String fileName) throws IOException {
        Path filePath = currentDirectory.resolve(fileName);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
        	System.out.println("Erreur: Le fichier " + fileName + " n'existe pas ou est inaccessibile.");
            return;
        }

        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream(filePath.toFile())) {
            dos.writeLong(filePath.toFile().length());
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }
            fis.close();
            System.out.println("Le fichier " + fileName + " a bien ete envoye");
        } catch (IOException e) {
        	System.out.println("Erreur lors du envoie du fichier: " + e.getMessage());
        }
    }

    private void delete(String name) {
        try {
            Path pathToDelete = currentDirectory.resolve(name);
            if (Files.exists(pathToDelete)) {
                Files.delete(pathToDelete); 
                System.out.println("Deleted: " + pathToDelete);
            } else {
                System.out.println("File or directory does not exist: " + pathToDelete);
            }
        } catch (IOException e) {
            System.err.println("Error deleting file/directory: " + e.getMessage());
        }
    }


    public void run() {
        try {
            // Création du canal d'envoi vers le client
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.writeUTF("Hello from server - you are client#" + clientNumber); // Envoi du message au client

            String clientCommand;
            String clientAddress = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();
            while (true) {
            	clientCommand = in.readUTF();
                if (clientCommand == null) {
                    break;
                }
                
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");
                Date date = new Date();
                System.out.println("[" + clientAddress + ":" + clientPort + " - " + dateFormat.format(date) + "]" + " : " + clientCommand);
                
                String[] command = command(clientCommand);
                handleCommand(command);
            }
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
