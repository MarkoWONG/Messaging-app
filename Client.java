
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
                            messageFromChat.matches("(.*)press enter to decline: $")
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
                            peerSocket = new Socket("localhost", targetPort); //connected if the other end .accept() this new connection
                            
                            peerBufferedWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                            peerBufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                            listenForPrivateMessage();
                            peerBufferedWriter.write("first msg sent");
                            peerBufferedWriter.newLine();
                            peerBufferedWriter.flush();
                            System.out.println("sent tests message");
                            // System.out.println("peersocket port is "+ peerSocket.getPort());
                            // System.out.println("peersocket localport is "+ peerSocket.getLocalPort());
                           
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

    //listen for Peer wanting to connect to this peer's port
    public void startServer(){ // start on private request
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{// use a welcoming port TODO
                    ServerSocket serverSocket = new ServerSocket(socket.getLocalPort());
                    while (!serverSocket.isClosed()){
                        peerSocket = serverSocket.accept();
                        System.out.println("Peer 2 Peer connected!"+ serverSocket.getLocalPort() + "-->" + peerSocket.getPort());

                        peerBufferedWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                        peerBufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                        listenForPrivateMessage();
                        peerBufferedWriter.write("Welcome Test message");
                        peerBufferedWriter.newLine();
                        peerBufferedWriter.flush();
                        System.out.println("sent message");
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
    
    public void listenForPrivateMessage(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String messageFromChat;
                while(peerSocket.isConnected() && !peerSocket.isClosed() ){
                    try{
                        System.out.println("waiting for private message");
                        messageFromChat = peerBufferedReader.readLine();
                        System.out.println("received a private message");
                        System.out.println(messageFromChat);
                    }
                    catch(IOException e){
                        closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                    }
                    catch(NullPointerException e){
                        closeEverything(peerSocket, peerBufferedReader, peerBufferedWriter);
                    }
                }
                System.err.println("Stopped listening to private messages");
            }
        }).start();
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

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        // acquire port number from command line parameter
        Integer serverPort = Integer.parseInt(args[1]);
        Socket socket = new Socket(args[0], serverPort);
        Client client = new Client(socket);
        client.listenForMessage();
        client.startServer();
        client.sendMessage();
        scanner.close();
    }
}
