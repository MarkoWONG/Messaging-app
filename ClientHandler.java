import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import jdk.javadoc.doclet.Taglet;

import java.time.LocalTime;
import java.util.Arrays;
// import static java.time.temporal.ChronoUnit.SECONDS;;

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
            clientHandlers.add(this);
            this.clientLoggedIn = false;
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
                // Login
                if (clientLoggedIn == false){
                    login();
                }
                // execute commands from client once logged in
                else{
                    commandHandler(message);
                    
                }
            }
            catch (IOException e){
                e.printStackTrace();
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
        
    }

    private void login() throws IOException{
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
            loginSuccessful();
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
                sendMessage("Your account is blocked due to multiple login failures. Please try again after: " + server.getBlockOut() + " seconds");
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
                        loginSuccessful();
                        
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

    private void loginSuccessful(){
        account.setLoggedIn(true);
        account.setActiveSocket(socket);
        account.setLastLoginTime(LocalTime.now());
        sendMessage("Welcome to the greatest messaging application ever!");
        broadcastMessage("logged in");
        clientLoggedIn = true;
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

    private void commandHandler(String message) throws IOException{
        LocalTime timeOutTimer = LocalTime.now();
        boolean timedOut = true;
        // Time out logic
        System.out.println(account.getUsername() + "'s Timer (Re)Started At " + LocalTime.now().toString());
        while (timeOutTimer.plusSeconds(server.getTimeOut()).compareTo(LocalTime.now()) > 0) {
            if (bufferedReader.ready()) {
                message = bufferedReader.readLine();
                // String command = message.split(" ",2)[0];
                if (checkCommand(message)){
                    timedOut = false;
                    break;
                }
                else{
                    sendMessage("Error. Invalid command");
                }
            }
        }
        if (timedOut){
            sendMessage("Inactivity Detected, " + server.getTimeOut() +" since last command. Please login again. Press enter to quit");
            System.out.println("inactivity msg sent");
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
        // Valid Commands
        else{
            if (message.equals("logout")){
                System.out.println(account.getUsername() +  " logged out");
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
            else if (message.matches("^broadcast (.*)")){
                message = message.split(" ",2)[1];
                broadcastMessage(message);
            }
            else if (message.matches("^whoelsesince (.*)")){
                Integer time = Integer.parseInt(message.split(" ",2)[1]);
                whoelsesince(time);
            }
            else if (message.matches("^whoelse")){
                whoelse();
            }
            else if (message.matches("^message (.*)")){
                message = message.split(" ",2)[1];
                messagePerson(message);
            }
            else{
                sendMessage("Error. Invalid command");
                System.out.println(account.getUsername() + " Unknown request");
            }
        }
    }

    private boolean checkCommand(String msg){
        // whoelsesince message needs to be a number
        if (msg.matches("^whoelsesince (.+)")){
            try {
                Integer.parseInt(msg.split(" ",2)[1]);
                return true;
            }
            catch (NumberFormatException e){
                return false;
            }
        }
        else if (msg.matches("^message (.+) (.+)")){
            String userName = msg.split(" ",3)[1];
            if (existingUser(userName) != null){
                return true;
            }
            else{
                return false;
            }
        }
        else if (msg.matches("^block (.+)")){
            String userName = msg.split(" ",2)[1];
            if (existingUser(userName) != null){
                return true;
            }
            else{
                return false;
            }
        }
        else if (msg.matches("^unblock (.+)")){
            String userName = msg.split(" ",2)[1];
            if (existingUser(userName) != null && !userInBlockList(userName)){
                return true;
            }
            else{
                return false;
            }
        }
        // no further check required beside the regex (logout, broadcast, whoelse)
        else if (msg.matches("^logout$") || msg.matches("^broadcast (.+)") || msg.matches("^whoelse$")){
            return true;
        }
        else{
            return false;
        }
    }
    
    private Account existingUser(String userName){
        for (Account acc : server.getAccounts()){
            if (acc.getUsername().equals(userName)){
                return acc;
            }
        }
        return null;
    }

    private boolean userInBlockList(String userName) {
        // TODO: When implmenting blocking users
        // for (Account acc : account.getBlockedList()){
        //     if (acc.getUsername().equals(userName)){
        //         return true;
        //     }
        // }
        return false;
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

    private void broadcastMessage(String message){
        for (ClientHandler clientHandler : clientHandlers){
            try{
                // don't send to self or client not logged in
                if (
                    clientHandler.account != null && 
                    !clientHandler.account.getUsername().equals(this.account.getUsername())
                ){
                    if (message.matches("^logged out$") || message.matches("^logged in$")){
                        clientHandler.bufferedWriter.write(this.account.getUsername() + " " + message);
                    }
                    else{
                        clientHandler.bufferedWriter.write(this.account.getUsername() + ": " + message);
                    }
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            }
            catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void whoelse() {
        for (Account acc : server.getAccounts()){
            if (acc.getLoggedIn() && acc != account){
                sendMessage(acc.getUsername());
            }
        }
    }

    private void whoelsesince(int time) {
        for (Account acc : server.getAccounts()){
            if (
                acc != account && 
                acc.getLastLoginTime() != null && 
                acc.getLastLoginTime().compareTo(LocalTime.now().minusSeconds(time)) > 0
            ){
                sendMessage(acc.getUsername());
            }
        }
    }

    private void messagePerson(String msg){
        String targetName = msg.split(" ", 2)[0];
        String message = msg.split(" ", 2)[1];
        Account target = existingUser(targetName);
        try{
            // don't send to self or client not logged in
            for (ClientHandler clientHandler : clientHandlers){
                if (clientHandler.account == target){
                    clientHandler.bufferedWriter.write(this.account.getUsername() + ": " + message);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }


    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
        account.setLoggedIn(false);
        account.setActiveSocket(null);
        clientLoggedIn = false;
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
    private void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage("logged out");
    }
}
