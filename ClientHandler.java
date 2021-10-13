import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.time.LocalTime;

public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Account account;
    private Server server;
    private boolean clientLoggedIn;

    public ClientHandler(Server server, Socket socket){
        try{
            this.server = server;
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this);
            this.clientLoggedIn = false;
            //broadcastMessage("SERVER: " + clientUsername + "has entered the chat");
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String message;

        while (socket.isConnected()){
            try{
                // Login
                if (clientLoggedIn == false){
                    // Prompt user for username
                    sendMessage("Username: ");
                    String userNameInput = bufferedReader.readLine();
                    
                    // check username
                    // Create new account
                    if (!server.exisitingAccount(userNameInput)){
                        // prompt for password
                        sendMessage("This is a new user. Enter a password: ");
                        String passwordInput = bufferedReader.readLine();

                        // create new entry for new account
                        account = createNewAccount(userNameInput, passwordInput);
                        account.setLoggedIn(true);
                        account.setActiveSocket(socket);
                        clientLoggedIn = true;
                        sendMessage("Welcome to the greatest messaging application ever!");
                    }
                    // login into existing account
                    else{
                        account = server.findAccount(userNameInput);
                        // Allow only one connection to each account
                        if (account.getLoggedIn()){
                            sendMessage(userNameInput + " is already in use. Please use another account.");
                        }
                        // Check if account is locked out
                        else if ( LocalTime.now().compareTo(account.getLockedOutFinishTime()) < 0){
                            sendMessage( "Your account is blocked due to multiple login failures. Please try again after: " + server.getBlockOut() + " seconds");
                        }
                        // account is not locked out
                        else {
                            // check password
                            boolean activeBlockout = false;
                            for (int attempts = 0; attempts < 3; attempts++){
                                // prompt for password
                                sendMessage("Password: ");
                                String passwordInput = bufferedReader.readLine();

                                //correct password
                                if (server.checkPassword(userNameInput, passwordInput)){
                                    activeBlockout = false;
                                    account.setLoggedIn(true);
                                    account.setActiveSocket(socket);
                                    sendMessage("Welcome to the greatest messaging application ever!");
                                    clientLoggedIn = true;
                                    break;
                                }
                                // incorrect password
                                sendMessage("Incorrect password. Please try again. Attempts left: " + (3 - attempts));
                                activeBlockout = true;
                            }
                            if (activeBlockout){
                                // Lock up the account
                                sendMessage("Your account is blocked due to multiple login failures. Please try again after: " + server.getBlockOut() + " seconds");
                                account.setLockedOutFinishTime(LocalTime.now().plusSeconds(server.getBlockOut()));
                            }
                        }
                    }
                }
                else{
                    LocalTime timeOutTimer = LocalTime.now();
                    boolean timedOut = true;
                    System.out.println("waiting");
                    while (timeOutTimer.plusSeconds(server.getTimeOut()).compareTo(LocalTime.now()) > 0) {
                        if (bufferedReader.ready()) {
                            timedOut = false;
                            break;
                        }
                    }
                    if (timedOut){
                        sendMessage("Inactivity Detected. Please login again. After: " + server.getTimeOut() + " seconds");
                        System.out.println("inactivity msg sent");
                        account.setLoggedIn(false);
                        account.setActiveSocket(null);
                        clientLoggedIn = false;
                    }
                    else{
                        // System.out.println("Awaiting messages");
                        message = bufferedReader.readLine();
                        if (message.equals("logout")){
                            account.setLoggedIn(false);
                            System.out.println(account.getUsername() + " has logged out");
                            closeEverything(socket, bufferedReader, bufferedWriter);
                        }
                        broadcastMessage(message);
                    }
                }
            }
            catch (IOException e){
                //e.printStackTrace();
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
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

    /**
     * Creates a new account by appending new account detials to credentials file
     * @param userName
     * @param password 
     */
    private Account createNewAccount(String userName, String password) throws IOException{
        try(
            FileWriter credFile = new FileWriter("credentials.txt", true);
            BufferedWriter credFileWriter = new BufferedWriter(credFile);
            PrintWriter append = new PrintWriter(credFileWriter)
        ){
            append.println(userName + " " + password);
            Account acc = new Account(userName, password);
            server.getAccounts().add(acc);
            return acc;
        } 
        catch (IOException e) {
            throw new IOException("credentials.txt was not found");
        }
    }

    public void broadcastMessage(String message){
        for (ClientHandler clientHandler : clientHandlers){
            try{
                if (clientHandler.account != null && !clientHandler.account.getUsername().equals(this.account.getUsername())){
                    clientHandler.bufferedWriter.write(this.account.getUsername() + ": " + message);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            }
            catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage(account.getUsername() +  " has left the chat");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
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
