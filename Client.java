
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

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
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
                if (message.equals("logout")){
                    System.out.println("logging out");
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
                            messageFromChat.matches("^[Pp]assword:(.*)") ||
                            messageFromChat.matches("^Username:(.*)")
                        ){
                            System.out.print(messageFromChat);
                        }
                        else if (messageFromChat.matches("^Inactivity(.*)")){
                            System.out.println(messageFromChat);
                            closeEverything(socket, bufferedReader, bufferedWriter);
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
        Socket socket = new Socket("localhost", 8000);
        Client client = new Client(socket);
        client.listenForMessage();
        client.sendMessage();
        scanner.close();
    }
}
