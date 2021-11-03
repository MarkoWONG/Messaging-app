
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Socket peerSocket;
    private BufferedReader peerBufferedReader;
    private BufferedWriter peerBufferedWriter;
    private String accountName;
    private String peerName;
    private ServerSocket serverSocket;

    public Client(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendMessage(){
        Scanner scanner = new Scanner(System.in);
        try{
            while (socket.isConnected() && !socket.isClosed() ){
                String message = scanner.nextLine();
                if (message.equals("logout")){
                    if (peerSocket != null){
                        stopprivate();
                    }
                    bufferedWriter.write(message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
                else if (
                    message.matches("^private(.*)") ||
                    message.matches("^stopprivate(.*)")
                ){
                    boolean validCommand = checkCommand(message);
                    if (validCommand && message.matches("^private(.*)")){
                        message = message.split(" ", 3)[2];
                        peerBufferedWriter.write(accountName + " (private): " + message);
                        peerBufferedWriter.newLine();
                        peerBufferedWriter.flush();
                        bufferedWriter.write("private valid");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                    else if (validCommand && message.matches("^stopprivate(.*)")){
                        stopprivate();
                        // System.out.println("Stopped private messaging with " + peerName);
                        // peerBufferedWriter.write("Close "+accountName+" Peer2Peer Server");
                        // peerBufferedWriter.newLine();
                        // peerBufferedWriter.flush();
                        // peerName = null;
                        // closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                        // bufferedWriter.write("private valid");
                        // bufferedWriter.newLine();
                        // bufferedWriter.flush();
                    }
                    else{
                        bufferedWriter.write("private invalid");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                }
                else {
                    bufferedWriter.write(message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
                
            }
        }
        catch (IOException e){
            scanner.close();
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void stopprivate(){
        try{
            peerBufferedWriter.write("Close "+accountName+" Peer2Peer Server");
            peerBufferedWriter.newLine();
            peerBufferedWriter.flush();
            peerName = null;
            serverSocket.close();
            closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
            bufferedWriter.write("private valid");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        catch (IOException e){
            closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
        }
    }

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
                        else if (
                            msg.matches("^Inactivity(.*)") ||
                            msg.matches("^Your account is blocked(.*)")
                        ){
                            System.out.println(msg);
                            closeEverything(socket, bufferedReader, bufferedWriter);
                        }
                        else if (msg.matches("^Client-Info: (.+)")){
                            Integer targetPort = Integer.parseInt(msg.split(" ")[1]);
                            peerSocket = new Socket("localhost", targetPort); //connected if the other end .accept() this new connection
                            
                            peerBufferedWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                            peerBufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                            listenForPrivateMessage();
                            // peerBufferedWriter.write("first msg sent to PeerSever");
                            // peerBufferedWriter.newLine();
                            // peerBufferedWriter.flush();
                        }
                        else if (msg.matches("^Welcome (.*)")){
                            accountName = (msg.split(" ", 3)[1]);
                            System.out.println(msg);
                        }
                        else if (msg.matches("(.+) accepted your private messaging request$")){
                            peerName = msg.split(" ", 2)[0];
                            System.out.println(msg);
                        }
                        else if (msg.matches("(.+) press enter to decline: $")){
                            peerName = msg.split(" ", 2)[0];
                            System.out.print(msg);
                        }
                        else if (msg.matches("^Start (.+)")){
                            peerName = msg.split(" ", 3)[1];
                            startServer();
                        }
                        else{
                            System.out.println(msg);
                        }
                    }
                    catch(IOException e){
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                    catch(NullPointerException e){
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    //listen for Peer wanting to connect to this peer's port
    public void startServer(){ // start on private request
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    serverSocket = new ServerSocket(socket.getLocalPort());
                    while (!serverSocket.isClosed()){
                        peerSocket = serverSocket.accept();
                        // System.out.println("Peer 2 Peer connected!"+ serverSocket.getLocalPort() + "-->" + peerSocket.getPort());

                        peerBufferedWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                        peerBufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                        listenForPrivateMessage();
                        // peerBufferedWriter.write("Welcome Test message from PeerSever");
                        // peerBufferedWriter.newLine();
                        // peerBufferedWriter.flush();
                    }
                }
                catch (IOException e){
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }).start();
    }
    
    public void listenForPrivateMessage(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String msg;
                while(peerSocket.isConnected() && !peerSocket.isClosed() ){
                    try{
                        msg = peerBufferedReader.readLine();
                        if (msg.matches("^Close (.*)")){
                            peerBufferedWriter.write("Close "+accountName+" Peer2Peer Server");
                            peerBufferedWriter.newLine();
                            peerBufferedWriter.flush();
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
                System.err.println("Peer2Peer messaging is now Closed");
            }
        }).start();
    }

    private boolean checkCommand(String message){
        if (message.matches("^private (.+) (.+)")){
            // Check if correct user
            String user = message.split(" ",3)[1];
            // System.out.println("Comparing " + user + " to " + peerName );
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
        Scanner scanner = new Scanner(System.in);
        // acquire port number from command line parameter
        Integer serverPort = Integer.parseInt(args[0]);
        Socket socket = new Socket("localhost", serverPort);
        Client client = new Client(socket);
        client.listenForMessage();
        client.sendMessage();
        scanner.close();
    }

}
