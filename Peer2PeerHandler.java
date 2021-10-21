import java.io.*;
import java.net.Socket;

public class Peer2PeerHandler implements Runnable {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    //private Account account;

    public Peer2PeerHandler(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void run() {
        // String message = "";
        // while (socket.isConnected() && !socket.isClosed()){
        //     try{
        //         // execute commands from client once logged in
        //         commandHandler(message);
        //     }
        //     catch (IOException e){
        //         e.printStackTrace();
        //         closeEverything(socket, bufferedReader, bufferedWriter);
        //         break;
        //     }
        // }
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
            e.printStackTrace();
        }
    }
}
