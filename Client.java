
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;

public class Client {
    // Attributes
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Socket peerSocket;
    private BufferedReader peerBufferedReader;
    private BufferedWriter peerBufferedWriter;
    private String accountName;
    private String peerName;
    private ServerSocket serverSocket;
    private boolean awaitingConnection;

    /**
     * Constructor For Client
     * @param socket
     */
    public Client(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.awaitingConnection = false;
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Continously listen to message sent by Server in a new thread
    public void listenForMessage(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String msg;
                while(socket.isConnected() && !socket.isClosed() ){
                    try{
                        msg = bufferedReader.readLine();
                        if (
                            msg.matches("^Password:(.*)") ||
                            msg.matches("^This is a new user(.*)") ||
                            msg.matches("^Username:(.*)")
                        ){
                            System.out.print(msg);
                        }
                        else if (msg.matches("(.+) press enter to decline: $")){
                            awaitingConnection = true;
                            System.out.print(msg);
                        }
                        else if (
                            msg.matches("^Inactivity(.*)") ||
                            msg.matches("^Your account is blocked(.*)")
                        ){
                            System.out.println(msg);
                            closeEverything(socket, bufferedReader, bufferedWriter);
                        }
                        else if (msg.matches("^Client-Info: (.+)")){
                            Integer targetPort = Integer.parseInt(msg.split(" ", 2)[1]);
                            // connect to Peer
                            // System.out.println("attempting toconncet on port: " + targetPort);
                            peerSocket = new Socket("localhost", targetPort);
                            peerBufferedWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                            peerBufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                            listenForPrivateMessage();
                        }
                        else if (msg.matches("^Welcome (.*)")){
                            accountName = (msg.split(" ", 3)[1]);
                            System.out.println(msg);
                        }
                        else if (msg.matches("(.+) accepted your private messaging request$")){
                            peerName = msg.split(" ", 2)[0];
                            System.out.println(msg);
                        }
                        else if (msg.matches("^Connection with (.+)")){
                            peerName = msg.split(" ", 4)[2];
                            System.out.println("accepted private messaging with: " + peerName);
                        }
                        else{
                            System.out.println(msg);
                        }
                    }
                    catch(IOException e){
                        //e.printStackTrace();
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                    catch(NullPointerException e){
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    // Continously reads input from user
    public void inputHandler(){
        Scanner scanner = new Scanner(System.in);
        while (socket.isConnected() && !socket.isClosed() ){
            String message = scanner.nextLine();
            if (message.equals("logout")){
                if (peerSocket != null){
                    stopprivate();
                }
                sendMessage(socket, bufferedReader, bufferedWriter, message);
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
            else if (
                message.matches("^private(.*)") ||
                message.matches("^stopprivate(.*)")
            ){
                boolean validCommand = checkCommand(message);
                if (validCommand && message.matches("^private(.*)")){
                    privateMsg(message);
                }
                else if (validCommand && message.matches("^stopprivate(.*)")){
                    stopprivate();
                }
                else{
                    sendMessage(socket, bufferedReader, bufferedWriter, "private invalid");
                }
            }
            else if (message.equals("y") && awaitingConnection){
                try {
                    serverSocket = new ServerSocket(0);
                    awaitingConnection = false;
                }
                catch (IOException e){
                    e.printStackTrace();
                    System.out.println("Couldn't start Server");
                }
                startServer();
                sendMessage(socket, bufferedReader, bufferedWriter, (message + " " + serverSocket.getLocalPort()));
            }
            else {
                if (awaitingConnection){
                    awaitingConnection = false;
                    System.out.println("Declined private messaging");
                }
                sendMessage(socket, bufferedReader, bufferedWriter, message);
            }
        }
        scanner.close();
    }

    /**
     * Send Message to the appropriate recipient 
     * @param soc
     * @param reader
     * @param writer
     * @param message
     */
    private void sendMessage(Socket soc, BufferedReader reader, BufferedWriter writer, String message){
        try{
            writer.write(message);
            writer.newLine();
            writer.flush();
        }
        catch(IOException e){
            closeEverything(soc, reader, writer);
        }
    }

    /**
     * check for if the given command is valid
     * @param message
     * @return Validity of Command
     */
    private boolean checkCommand(String message){
        if (message.matches("^private (.+) (.+)")){
            // Check if correct user
            String user = message.split(" ",3)[1];
            if (user.equals(peerName)){
                return true;
            }
            System.out.println("Error. Private messaging to "+ user +" not enabled");
            return false;
        }
        else if (message.matches("^stopprivate (.+)")){
            // Check if correct user
            String user = message.split(" ",2)[1];
            if (user.equals(peerName)){
                return true;
            }
            System.out.println("Error. Private messaging to "+ user +" was never enabled");
            return false;
        }
        else if (message.matches("^private(.*)")){
            System.out.println("Error. Usage: private <User> <Message>");
            return false;
        }
        else if (message.matches("^stopprivate(.*)")){
            System.out.println("Error. Usage: stopprivate <User>");
            return false;
        }
        else {
            System.out.println("Error. Invalid command");
            return false;
        }
    }

    /**
     * Private message command logic: send message to peer and send valid message to server to mainatain timeout
     * @param message
     */
    private void privateMsg(String message){
        message = message.split(" ", 3)[2];
        sendMessage(peerSocket, peerBufferedReader, peerBufferedWriter, accountName + " (private): " + message);
        sendMessage(socket, bufferedReader, bufferedWriter, "private valid");
    }

    // Stop Peer 2 Peer connection
    private void stopprivate(){
        try{
            sendMessage(peerSocket, peerBufferedReader, peerBufferedWriter, "Close "+accountName+" Peer2Peer Server");
            peerName = null;
            if (serverSocket != null){
                serverSocket.close();
            }
            closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
            sendMessage(socket, bufferedReader, bufferedWriter, "private valid");
        }
        catch (IOException e){
            closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
        }
    }

    // listen for Peer wanting to connect to this peer's port
    public void startServer(){ 
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    while (!serverSocket.isClosed()){
                        peerSocket = serverSocket.accept();
                        peerBufferedWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                        peerBufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                        listenForPrivateMessage();
                    }
                }
                catch (IOException e){
                    //e.printStackTrace();
                    closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                }
            }
        }).start();
    }
    
    // Continously listen for private messages
    public void listenForPrivateMessage(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String msg;
                String copyPeerName = peerName;
                while(peerSocket.isConnected() && !peerSocket.isClosed() ){
                    try{
                        msg = peerBufferedReader.readLine();
                        if (msg.matches("^Close (.*)")){
                            sendMessage(peerSocket, peerBufferedReader, peerBufferedWriter, "Close "+accountName+" Peer2Peer Server");
                            peerName = null;
                            serverSocket.close();
                            closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                        }
                        else{
                            System.out.println(msg);
                        }
                    }
                    catch(IOException e){
                        closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                    }
                    catch(NullPointerException e){
                        closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                    }
                }
                System.err.println("Closed private messaging with " + copyPeerName);
            }
        }).start();
    }

    /**
     * Close the socket and read/write stream
     * @param socket
     * @param bufferedReader
     * @param bufferedWriter
     */
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        try{
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
            if (socket != null){
                socket.close();
            }
        }
        catch (IOException e){
            //e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // acquire port number from command line parameter
        Integer serverPort = Integer.parseInt(args[0]);

        // Connect with Server/ get Socket to communicate with Server
        Socket socket = new Socket("localhost", serverPort);

        // Start Client functions listening to server and reading input
        Client client = new Client(socket);
        client.listenForMessage();
        client.inputHandler();
    }
}
