import java.io.*;
import java.net.Socket;
// import java.util.ArrayList;
// import java.util.List;

// import java.time.LocalTime;
// import java.util.Arrays;
// import static java.time.temporal.ChronoUnit.SECONDS;;

public class Peer2PeerHandler implements Runnable {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    // private Server server;
    

    public Peer2PeerHandler(Socket socket){
        try{
            // this.server = server;
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String message = "";

        while (socket.isConnected() && !socket.isClosed()){
            try{
                commandHandler(message);
            }
            catch (IOException e){
                e.printStackTrace();
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
        
    }

    private void commandHandler(String message) throws IOException{
        message = bufferedReader.readLine();
        System.out.println("Message: "+ message);
        // Valid Commands
        if (message.matches("^private (.*)")){
            try {
                message = message.split(" ")[2];
                bufferedWriter.write("Test message");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                System.out.println("sent messenge");
            }
            catch (IOException e){
                e.printStackTrace();
            } 
        }
        else if (message.matches("^stopprivate (.*)")){
            //TODO: 
            //close peer2peer connection
        }
        else{
            sendMessage("Error. Invalid command");
            System.out.println("Peer Unknown request of: " + message);
        }
    }
    private void sendMessage(String message){
        try{
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
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
            e.printStackTrace();
        }
    }

}
