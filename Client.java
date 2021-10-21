
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
                Boolean valid_command = false;
                if (
                    message.matches("^private (.*)") ||
                    message.matches("^stopprivate (.*)")
                ){
                    valid_command = checkCommand(message);
                    peer2peer(message);
                }
                // if peer 2 peer command is invlaid send the invlaid command to server is continue timeout
                if (!valid_command){
                    bufferedWriter.write(message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
                if (message.equals("logout")){
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
        catch (IOException e){
            scanner.close();
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void listenForMessage(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String messageFromChat;

                while(socket.isConnected() && !socket.isClosed() ){
                    try{
                        messageFromChat = bufferedReader.readLine();
                        if (
                            messageFromChat.matches("^Password:(.*)") ||
                            messageFromChat.matches("^This is a new user(.*)") ||
                            messageFromChat.matches("^Username:(.*)") ||
                            messageFromChat.matches("(.+)press enter to decline:{1}")
                        ){
                            System.out.print(messageFromChat);
                        }
                        else if (
                            messageFromChat.matches("^Inactivity(.*)") ||
                            messageFromChat.matches("^Your account is blocked(.*)")
                        ){
                            System.out.println(messageFromChat);
                            closeEverything(socket, bufferedReader, bufferedWriter);
                        }
                        else if (messageFromChat.matches("^Client-Info: (.+)")){
                            Integer targetPort = Integer.parseInt(messageFromChat.split(" ")[1]);
                            peerSocket = new Socket("localhost", targetPort);
                            peerBufferedWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                            // peerBufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                        }
                        else{
                            System.out.println(messageFromChat);
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

    public void listenForPrivateMessage(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String messageFromChat;

                while(peerSocket.isConnected() && !peerSocket.isClosed() ){
                    try{
                        messageFromChat = peerBufferedReader.readLine();
                        System.out.println(messageFromChat);
                    }
                    catch(IOException e){
                        closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                    }
                    catch(NullPointerException e){
                        closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                    }
                }
            }
        }).start();
    }

    // listen for Peer wanting to connect to this peer's port
    public void startServer(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    ServerSocket serverSocket = new ServerSocket(socket.getLocalPort());
                    while (!serverSocket.isClosed()){
                        Socket socket = serverSocket.accept();
                        System.out.println("Peer 2 Peer connected!");
                        // Peer2PeerHandler peer2PeerHandler = new Peer2PeerHandler(socket);

                        // Thread thread = new Thread(peer2PeerHandler);
                        // thread.start();
                    }
                }
                catch (IOException e){
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }).start();
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
            if (peerSocket != null){
                peerSocket.close();
            }
        }
        catch (IOException e){
            //e.printStackTrace();
        }
    }
    
    /**
     * peer2peer logic 
     * @pre valid command (user is correct)
     * @param message
     * @return true for a valid peer2peer message, false otherwise
     */
    public void peer2peer(String message){
        if (message.matches("^private (.*)")){
            try {
                message = message.split(" ")[2];
                peerBufferedWriter.write(message);
                peerBufferedWriter.newLine();
                peerBufferedWriter.flush();
            }
            catch (IOException e){
                e.printStackTrace();
            } 
        }
        else if (message.matches("^stopprivate (.*)")){
            //TODO: 
            //close peer2peer connection
        }
    }

    private boolean checkCommand(String message){
        if (message.matches("^private (.+) (.+)")){
            //TODO: 
            return true;
        }
        else if (message.matches("^stopprivate (.*)")){
            //TODO: 
            return true;
        }
        else {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        // acquire port number from command line parameter
        Integer serverPort = Integer.parseInt(args[1]);
        Socket socket = new Socket(args[0], serverPort);
        Client client = new Client(socket);
        client.listenForMessage();
        client.startServer();
        client.listenForPrivateMessage();
        client.sendMessage();
        scanner.close();
    }
}
