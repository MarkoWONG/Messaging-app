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
import java.io.*;
import java.util.Scanner;

public class Server {

    // Server information
    private static ServerSocket serverSocket;
    private static Integer serverPort;
    private static Integer blockOut;
    private static Integer timeOut;
    

    // define ClientThread for handling multi-threading issue
    // ClientThread needs to extend Thread and override run() method
    private static class ClientThread extends Thread {
        private final Socket clientSocket;
        private boolean clientAlive = false;
        private boolean loggedIn = false;

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
                        send_message(dataOutputStream, clientID, "Username: ");
                        String userNameInput = (String) dataInputStream.readUTF();
                        
                        // check username
                        if (!exisitingUser(userNameInput)){
                            // prompt for password
                            send_message(dataOutputStream, clientID, "This is a new user. Enter a password: ");
                            String passwordInput = (String) dataInputStream.readUTF();

                            // create new entry for new user
                            createNewUser(userNameInput, passwordInput);
                        }
                        else{
                            // Keep checking password until correct
                            while (true){
                                // check password
                                boolean activeBlockout = false;
                                for (int attempts = 0; attempts < 3; attempts++){
                                    // prompt for password
                                    send_message(dataOutputStream, clientID, "Password: ");
                                    String passwordInput = (String) dataInputStream.readUTF();

                                    //correct password
                                    if (checkPassword(userNameInput, passwordInput)){
                                        activeBlockout = false;
                                        break;
                                    }
                                    // incorrect password
                                    send_message(dataOutputStream, clientID, "Incorrect Password. Please try again. Attempts left: " + (3 - attempts));
                                    activeBlockout = true;
                                }
                                if (activeBlockout){
                                    send_message(dataOutputStream, clientID, "Your account is blocked due to multiple login failures. Please try again after: " + blockOut + " seconds");
                                    //Thread.sleep(blockOut * 1000);
                                    // Need to block account not client
                                }
                                else{
                                    break;
                                }
                            }
                        }
                        loggedIn = true;
                        send_message(dataOutputStream, clientID, "Welcome to the greatest messaging application ever!");
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
        private void send_message(DataOutputStream dataOutputStream, String clientID, String message) throws IOException{
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
         * Creates a new user by appending new user to credentials file
         * @param userName
         * @param password 
         */
        private void createNewUser(String userName, String password) throws IOException{
            try(FileWriter credFile = new FileWriter("credentials.txt", true);
                BufferedWriter credFileWriter = new BufferedWriter(credFile);
                PrintWriter append = new PrintWriter(credFileWriter))
            {
                append.println(userName + " " + password);
            } catch (IOException e) {
                throw new IOException("credentials.txt was not found");
            }
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
            // for each user there would be one thread, all the request/response for that user would be processed in that thread
            // different users will be working in different thread which is multi-threading (i.e., concurrent)
            ClientThread clientThread = new ClientThread(clientSocket);
            clientThread.start();
        }
    }

    
    // private void loginUser(){
    //     // Prompt user for username
    //     String responseMessage = "Please enter User Name: ";
    //     dataOutputStream.writeUTF(responseMessage);
    //     dataOutputStream.flush();
    //     // check username
    //         // create new entry for new user

    //     // prompt for password

    //         // check password
    // }
}