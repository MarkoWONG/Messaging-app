/*
 * Java socket programming client example with TCP
 * socket programming at the client side, which provides example of how to define client socket, how to send message to
 * the server and get response from the server with DataInputStream and DataOutputStream
 *
 * Author: Marko Wong
 * Date: 2021-10-10
 * */

import java.net.*;
import java.io.*;


public class Client {
    // server host and port number, which would be acquired from command line parameter
    private static String serverHost;
    private static Integer serverPort;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("===== Error usage: java TCPClient SERVER_IP SERVER_PORT =====");
            return;
        }

        serverHost = args[0];
        serverPort = Integer.parseInt(args[1]);

        // define socket for client
        Socket clientSocket = new Socket(serverHost, serverPort);

        // define DataInputStream instance which would be used to receive response from the server
        // define DataOutputStream instance which would be used to send message to the server
        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

        // define a BufferedReader to get input from command line i.e., standard input from keyboard
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        Boolean clientAlive = true;

        // Start Another Thread for listening to messages
        ListeningThread listeningThread = new ListeningThread(clientSocket);
        listeningThread.start();

        while (clientAlive) {
            // login
            String responseMessage = (String) dataInputStream.readUTF();
            if (responseMessage.equals("Username: ")){
                System.out.print("[recv] " + responseMessage);
                String userNameInput = reader.readLine();
                dataOutputStream.writeUTF(userNameInput);
                dataOutputStream.flush();
            }
            else if (responseMessage.matches("(.*)Incorrect Password(.*)")){
                System.out.println("[recv] " + responseMessage);
            }
            else if (responseMessage.matches("(.*)[Pp]assword(.*)")){
                System.out.print("[recv] " + responseMessage);
                String passwordInput = reader.readLine();
                dataOutputStream.writeUTF(passwordInput);
                dataOutputStream.flush();
            }
            else if (responseMessage.matches("(.*)blocked(.*)")){
                System.out.println("[recv] " + responseMessage);
                System.out.println("Good bye");
                clientSocket.close();
                dataOutputStream.close();
                dataInputStream.close();
                clientAlive = false;
            }
            else if (responseMessage.matches("(.*)Awaiting Commands(.*)")){
                // receive the server response from dataInputStream
                System.out.println("[recv] " + responseMessage);
                // read input from command line
                String message = reader.readLine();

                // write message into dataOutputStream and send/flush to the server
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
                if (message.equals("logout")){
                    clientAlive = false;
                }
            }
            else{
                System.out.println("[recv] " + responseMessage);
            }
        }
    }

    // define ClientThread for handling multi-threading issue
    // ClientThread needs to extend Thread and override run() method
    private static class ListeningThread extends Thread {
        private final Socket clientSocket;

        ListeningThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            super.run();
            String message;
            while (clientAlive){
                    String responseMessage = (String) dataInputStream.readUTF();
                    System.out.println("[recv] " + responseMessage);
            }
        }
    }
}