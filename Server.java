/*
 * Java multi-threading server with TCP
 * There are two points of this example code:
 * - socket programming with TCP e.g., how to define a server socket, how to exchange data between client and server
 * - multi-threading
 *
 * Author: Marko Wong
 * Date: 2021-10-10
 * */
import java.net.*;
import java.time.LocalTime;
import java.io.*;
import java.util.Scanner;
import java.util.List;

public class Server {

    // Server information
    private static ServerSocket serverSocket;
    private static Integer serverPort;
    private static Integer blockOut;
    private static Integer timeOut;
    private static List<Account> accounts;
    private static List<Account> activeAccounts;

    // define ClientThread for handling multi-threading issue
    // ClientThread needs to extend Thread and override run() method
    private static class ClientThread extends Thread {
        private final Socket clientSocket;
        private boolean clientAlive = false;
        private boolean loggedIn = false;
        private Account currentAccount = null;

        ClientThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            super.run();
            // get client Internet Address and port number
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();
            String clientID = "("+ clientAddress + ", " + clientPort + ")";

            System.out.println("===== New connection created for user - " + clientID);
            clientAlive = true;

            // define the dataInputStream to get message (input) from client
            // DataInputStream - used to acquire input from client
            // DataOutputStream - used to send data to client
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            try {
                dataInputStream = new DataInputStream(this.clientSocket.getInputStream());
                dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (clientAlive) {
                try {
                    // get input from client
                    // socket like a door/pipe which connects client and server together
                    // data from client would be read from clientSocket
                    assert dataInputStream != null;
                    assert dataOutputStream != null;

                    // Authenticate User
                   // loginUser();
                    if (loggedIn == false){
                        // Prompt user for username
                        send_message(dataOutputStream, "Username: ");
                        String userNameInput = (String) dataInputStream.readUTF();
                        
                        // check username
                        // Create new account
                        if (!exisitingUser(userNameInput)){
                            // prompt for password
                            send_message(dataOutputStream, "This is a new user. Enter a password: ");
                            String passwordInput = (String) dataInputStream.readUTF();

                            // create new entry for new account
                            currentAccount = createNewAccount(userNameInput, passwordInput);
                            loggedIn = true;
                            activeAccounts.add(currentAccount);
                            send_message(dataOutputStream, "Welcome to the greatest messaging application ever!");
                        }
                        // login into existing account
                        else{
                            currentAccount = findAccount(userNameInput);

                            // Check if account is locked out
                            if ( LocalTime.now().compareTo(currentAccount.getLockedOutFinishTime()) < 0){
                                send_message(dataOutputStream,  "Your account is blocked due to multiple login failures. Please try again after: " + blockOut + " seconds");
                            }
                            // account is not locked out
                            else {
                                // check password
                                boolean activeBlockout = false;
                                for (int attempts = 0; attempts < 3; attempts++){
                                    // prompt for password
                                    send_message(dataOutputStream, "Password: ");
                                    String passwordInput = (String) dataInputStream.readUTF();

                                    //correct password
                                    if (checkPassword(userNameInput, passwordInput)){
                                        activeBlockout = false;
                                        loggedIn = true;
                                        activeAccounts.add(currentAccount);
                                        send_message(dataOutputStream, "Welcome to the greatest messaging application ever!");
                                        break;
                                    }
                                    // incorrect password
                                    send_message(dataOutputStream, "Incorrect Password. Please try again. Attempts left: " + (3 - attempts));
                                    activeBlockout = true;
                                }
                                if (activeBlockout){
                                    // Lock up the account
                                    send_message(dataOutputStream, "Your account is blocked due to multiple login failures. Please try again after: " + blockOut + " seconds");
                                    currentAccount.setLockedOutFinishTime(LocalTime.now().plusSeconds(blockOut));
                                }
                            }
                        }
                    }
                    else{
                        String message = (String) dataInputStream.readUTF();
                        System.out.println("[recv]  " + message + " from user - " + clientID);
                        String responseMessage = "unknown request";
                        System.out.println("[send] " + message);
                        dataOutputStream.writeUTF(responseMessage);
                        dataOutputStream.flush();
                    }
                } 
                catch (EOFException e) {
                    System.out.println("===== the user disconnected, user - " + clientID);
                    clientAlive = false;
                } 
                catch (IOException e) {
                    System.out.println("===== the user disconnected, user - " + clientID);
                    clientAlive = false;
                }
                
            }
        }

        /**
         * Sends a message to Client
         * @param dataOutputStream
         * @param message
         */
        private void send_message(DataOutputStream dataOutputStream, String message) throws IOException{
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush();
        }

        /**
         * Checks if input matches the user/pass in the credential.txt
         * @param input
         * @return True for exisiting user/ correct password, false for new user/incorrect password
         */
        private boolean exisitingUser(String input) throws FileNotFoundException{
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

        private boolean checkPassword(String usernameInput, String passwordInput) throws FileNotFoundException{
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

        /**
         * Finds the account in the list of created accounts
         * @param userNameInput
         * @return
         */
        private Account findAccount(String userNameInput){
            for (Account acc : accounts){
                if (acc.getUsername().equals(userNameInput)){
                    return acc;
                }
            }
            return null;
        }
    }

    /**
     * Creates a new account by appending new account detials to credentials file
     * @param userName
     * @param password 
     */
    public static Account createNewAccount(String userName, String password) throws IOException{
        try(
            FileWriter credFile = new FileWriter("credentials.txt", true);
            BufferedWriter credFileWriter = new BufferedWriter(credFile);
            PrintWriter append = new PrintWriter(credFileWriter)
        ){
            append.println(userName + " " + password);
            Account acc = new Account(userName, password);
            accounts.add(acc);
            return acc;
        } 
        catch (IOException e) {
            throw new IOException("credentials.txt was not found");
        }
    }

    public static void popluateAccounts() throws IOException{
        try {
            File credFile = new File("credentials.txt");
            Scanner reader = new Scanner(credFile);
            while (reader.hasNextLine()) {
                String entry = reader.nextLine();
                String username = entry.split(" ")[0];
                String password = entry.split(" ")[1];
                Account acc = new Account(username, password);
                accounts.add(acc);
            }
            reader.close();
        } 
        catch (IOException e) {
            throw new IOException("credentials.txt was not found");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("===== Error usage: java Server SERVER_PORT BLOCK_DURATION TIMEOUT=====");
            return;
        }

        // acquire port number from command line parameter
        serverPort = Integer.parseInt(args[0]);
        // acquire block out duration (seconds) from command line parameter
        blockOut = Integer.parseInt(args[1]);
        // acquire time out duration (seconds) from command line parameter
        timeOut = Integer.parseInt(args[2]);


        // define server socket with the input port number, by default the host would be localhost i.e., 127.0.0.1
        serverSocket = new ServerSocket(serverPort);
        // make serverSocket listen connection request from clients
        System.out.println("===== Server is running =====");
        System.out.println("===== Waiting for connection request from clients...=====");

        while (true) {
            // when new connection request reaches the server, then server socket establishes connection
            Socket clientSocket = serverSocket.accept();

            // Populate existing accounts in the credentials.txt
            popluateAccounts();

            // for each user there would be one thread, all the request/response for that user would be processed in that thread
            // different users will be working in different thread which is multi-threading (i.e., concurrent)
            ClientThread clientThread = new ClientThread(clientSocket);
            clientThread.start();
        }
    }
}