
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class Server {
    private final ServerSocket serverSocket;
    private final Integer blockOut;
    private final Integer timeOut;
    private List<Account> accounts = new ArrayList<Account>();

    public Server(ServerSocket serverSocket, Integer blockOut, Integer timeOut){
        this.serverSocket = serverSocket;
        this.blockOut = blockOut;
        this.timeOut = timeOut;
    }

    /**
     * Fills the server with the accounts in the credentials.txt
     * @param server
     * @throws IOException
     */
    private static void popluateAccounts(Server server) throws IOException{
        try {
            File credFile = new File("credentials.txt");
            Scanner reader = new Scanner(credFile);
            while (reader.hasNextLine()) {
                String entry = reader.nextLine();
                String username = entry.split(" ")[0];
                String password = entry.split(" ")[1];
                Account acc = new Account(username, password);
                server.accounts.add(acc);
            }
            reader.close();
        } 
        catch (IOException e) {
            throw new IOException("credentials.txt was not found");
        }
    }

    // Start Server to wait for new connections
    public void startServer(){
        try{
            while (!serverSocket.isClosed()){

                Socket socket = serverSocket.accept();
                // System.out.println("A new client has connected!" + serverSocket.getLocalPort() + "-->" + socket.getPort());
                ClientHandler clientHandler = new ClientHandler(this, socket);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Finds the account in the list of created accounts
     * @param userNameInput
     * @return
     */
    public Account findAccount(String userNameInput){
        for (Account acc : accounts){
            if (acc.getUsername().equals(userNameInput)){
                return acc;
            }
        }
        return null;
    }

    /**
     * Checks if input matches the user/pass in the credential.txt
     * @param input
     * @return True for exisiting user/ correct password, false for new user/incorrect password
     */
    public boolean exisitingAccount(String input) throws FileNotFoundException{
        try {
            File credFile = new File("credentials.txt");
            Scanner reader = new Scanner(credFile);
            while (reader.hasNextLine()) {
                String entry = reader.nextLine();
                String username = entry.split(" ")[0];
                if (username.equals(input)){
                    reader.close();
                    return true;
                }
            }
            reader.close();
            return false;
        } 
        catch (FileNotFoundException e) {
            throw new FileNotFoundException("credentials.txt was not found");
        }
    }

    /**
     * check for correct password a user
     * @param usernameInput
     * @param passwordInput
     * @return if a password is correct
     * @throws FileNotFoundException
     */
    public boolean checkPassword(String usernameInput, String passwordInput) throws FileNotFoundException{
        try {
            File credFile = new File("credentials.txt");
            Scanner reader = new Scanner(credFile);
            while (reader.hasNextLine()) {
                String entry = reader.nextLine();
                String username = entry.split(" ")[0];
                String password = entry.split(" ")[1];
                if (username.equals(usernameInput) && password.equals(passwordInput)){
                    reader.close();
                    return true;
                }
            }
            reader.close();
            return false;
        } 
        catch (FileNotFoundException e) {
            throw new FileNotFoundException("credentials.txt was not found");
        }
    }
    
    public static void main(String[] args) throws IOException{
        if (args.length != 3) {
            System.out.println("===== Error usage: java Server SERVER_PORT BLOCK_DURATION TIMEOUT=====");
            return;
        }

        // acquire port number from command line parameter
        Integer serverPort = Integer.parseInt(args[0]);
        // acquire block out duration (seconds) from command line parameter
        Integer blockOut = Integer.parseInt(args[1]);
        // acquire time out duration (seconds) from command line parameter
        Integer timeOut = Integer.parseInt(args[2]);


        // define server socket with the input port number, by default the host would be localhost i.e., 127.0.0.1
        ServerSocket serverSocket = new ServerSocket(serverPort);

        // make serverSocket listen connection request from clients
        // System.out.println("===== Server is running =====");
        // System.out.println("===== Waiting for connection request from clients...=====");

        Server server = new Server(serverSocket, blockOut, timeOut);

        // Populate existing accounts in the credentials.txt
        popluateAccounts(server);

        server.startServer();

    }
    
    // Getters
    public Integer getBlockOut() {
        return this.blockOut;
    }

    public Integer getTimeOut() {
        return this.timeOut;
    }

    public List<Account> getAccounts() {
        return this.accounts;
    }
}

