import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
            loginSuccessful(account);
        }
        // login into existing account
        else{
            Account loggingInAccount = server.findAccount(userNameInput);
            // Allow only one connection to each account
            if (loggingInAccount.getLoggedIn()){
                sendMessage(userNameInput + " is already in use. Please use another account.");
            }
            // Check if account is locked out
            else if ( LocalTime.now().compareTo(loggingInAccount.getLockedOutFinishTime()) < 0){
                sendMessage("Your account is blocked due to multiple login failures. Please try again after: " + server.getBlockOut() + " seconds. Press Enter to quit");
                closeEverything(socket, bufferedReader, bufferedWriter);
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
                        loginSuccessful(loggingInAccount);
                        
                        break;
                    }
                    // incorrect password
                    sendMessage("Incorrect password. Please try again. Attempts left: " + (3 - attempts));
                    activeBlockout = true;
                }
                if (activeBlockout){
                    // Lock up the account
                    sendMessage("Your account is blocked due to multiple login failures. Please try again after: " + server.getBlockOut() + " seconds. Press Enter to quit");
                    loggingInAccount.setLockedOutFinishTime(LocalTime.now().plusSeconds(server.getBlockOut()));
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    private void loginSuccessful(Account acc){
        // set the client account to the logging in account as the client now has an account
        account = acc;
        account.setLoggedIn(true);
        account.setLastLoginTime(LocalTime.now());
        account.setActiveClient(this);
        sendMessage("Welcome "+ account.getUsername() +" to the greatest messaging application ever!");
        broadcastMessage("logged in");
        clientLoggedIn = true;
        //print all offline messages if there is any
        if (account.getOfflineMsgs().size() != 0){
            sendMessage("Messages you missed when your're offline:");
            for (String msg : account.getOfflineMsgs()){
                sendMessage(msg);
            }
            // remove all offline messages
            account.getOfflineMsgs().clear();
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
            }
        }
        if (timedOut){
            sendMessage("Inactivity Detected, " + server.getTimeOut() +" seconds since last command. Please login again. Press enter to quit");
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
            else if (message.matches("^block (.+)")){
                message = message.split(" ", 2)[1];
                blockAccount(message);
            }
            else if (message.matches("^unblock (.+)")){
                message = message.split(" ", 2)[1];
                unblockAccount(message);
            }
            else if (message.matches("^startprivate (.+)")){
                message = message.split(" ", 2)[1];
                startPrivate(message);
            }
            else if (message.matches("^private valid$")){

            }
            else{
                sendMessage("Error. Invalid command: " + message);
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
                sendMessage("Error. Time has to be a number");
                return false;
            }
        }
        else if (msg.matches("^message (.+) (.+)")){
            String userName = msg.split(" ",3)[1];
            if (existingUser(userName) != null && !userName.equals(account.getUsername())){
                return true;
            }
            else{
                sendMessage("Error. Invalid user");
                return false;
            }
        }
        else if (msg.matches("^block (.+)")){
            String userName = msg.split(" ",2)[1];
            if (existingUser(userName) != null && !account.getUsername().equals(userName)){
                return true;
            }
            else if (existingUser(userName) == null){
                sendMessage("Error. Invalid user");
                return false;
            }
            else if (account.getUsername().equals(userName)){
                sendMessage("Error. Cannot block self");
                return false;
            }
            else{
                sendMessage("Error. Invalid command");
                return false;
            }
        }
        else if (msg.matches("^unblock (.+)")){
            String userName = msg.split(" ",2)[1];
            if (existingUser(userName) != null && userInBlockList(userName, account.getBlockedAccounts())){
                return true;
            }
            else if (existingUser(userName) == null){
                sendMessage("Error. Invalid user");
                return false;
            }
            else if (!userInBlockList(userName, account.getBlockedAccounts())){
                sendMessage("Error. " + userName + " was not blocked");
                return false;
            }
            else{
                sendMessage("Error. Invalid command");
                return false;
            }
        }
        else if (msg.matches("^startprivate (.+)")){
            String userName = msg.split(" ",2)[1];
            if (
                existingUser(userName) != null && 
                existingUser(userName).getLoggedIn() &&
                !account.getUsername().equals(userName) &&
                !userInBlockList(account.getUsername(), existingUser(userName).getBlockedAccounts())
                ){
                return true;
            }
            else if (existingUser(userName) == null){
                sendMessage("Error. Invalid user");
                return false;
            }
            else if (!existingUser(userName).getLoggedIn()){
                sendMessage("Error. User is offline");
                return false;
            }
            else if (account.getUsername().equals(userName)){
                sendMessage("Error. Cannot privately message yourself");
                return false;
            }
            else if (!userInBlockList(account.getUsername(), existingUser(userName).getBlockedAccounts())){
                sendMessage("Error. " + userName + "has blocked you");
                return false;
            }
            else{
                sendMessage("Error. Invalid command");
                return false;
            }
        }
        // no further check required beside the regex (logout, broadcast, whoelse)
        else if (msg.matches("^logout$") || msg.matches("^broadcast (.+)") || msg.matches("^whoelse$")){
            return true;
        }
        else if (msg.matches("^private valid$")){
            return true;
        }
        else if (msg.matches("^private invalid$")){
            return false;
        }
        else{
            sendMessage("Error. Invalid command");
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

    //checks if the user is in the blocked list of this user
    private boolean userInBlockList(String userName, List<Account> blockedList) {
        for (Account acc : blockedList){
            if (acc.getUsername().equals(userName)){
                return true;
            }
        }
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
        Boolean blockedInEffect = false;
        for (ClientHandler clientHandler : clientHandlers){
            try{
                // don't send to self or client not logged in or if the receiver is blocked
                if (
                    clientHandler.account != null && 
                    !clientHandler.account.getUsername().equals(this.account.getUsername()) &&
                    !userInBlockList(this.account.getUsername(), clientHandler.account.getBlockedAccounts())
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
                else if (clientHandler.account != null && userInBlockList(this.account.getUsername(), clientHandler.account.getBlockedAccounts())){
                    blockedInEffect = true;
                }
            }
            catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
        if (blockedInEffect == true && !(message.matches("^logged out$") || message.matches("^logged in$"))){
            sendMessage("Your message could not be delivered to some recipients");
        }
    }

    private void whoelse() {
        for (Account acc : server.getAccounts()){
            if (acc.getLoggedIn() && acc != account && !userInBlockList(account.getUsername(), acc.getBlockedAccounts()) ){
                sendMessage(acc.getUsername());
            }
        }
    }

    private void whoelsesince(int time) {
        for (Account acc : server.getAccounts()){
            if (
                acc != account && 
                !userInBlockList(account.getUsername(), acc.getBlockedAccounts()) &&
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
            ClientHandler TClient = target.getActiveClient();
            if (TClient != null && !userInBlockList(account.getUsername(), target.getBlockedAccounts())){
                TClient.bufferedWriter.write(this.account.getUsername() + ": " + message);
                TClient.bufferedWriter.newLine();
                TClient.bufferedWriter.flush();
            }
            else if (userInBlockList(account.getUsername(), target.getBlockedAccounts())){
                sendMessage("Your message could not be delivered as the recipient has blocked you");
            }
            // offline messenging: store messenges in account then sent it all when user logs in
            else{
                target.getOfflineMsgs().add(this.account.getUsername() + ": " + message);
            }
        }
        catch (IOException e){
            e.printStackTrace();
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void blockAccount(String userName){
        sendMessage(userName + " is blocked");
        Account target = existingUser(userName);
        if (!account.getBlockedAccounts().contains(target)){
            account.getBlockedAccounts().add(target);
        }
    }

    private void unblockAccount(String userName){
        sendMessage(userName + " is unblocked");
        Account target = existingUser(userName);
        account.getBlockedAccounts().remove(target);
    }

    private void startPrivate(String userName){
        Account target = existingUser(userName);
        sendMessage("Start private messaging with " + userName);
        try{
            ClientHandler TClient = target.getActiveClient();
            TClient.bufferedWriter.write(this.account.getUsername() + " would like to private message, enter y to accept or press enter to decline: ");
            TClient.bufferedWriter.newLine();
            TClient.bufferedWriter.flush();
            // read response
            String response = TClient.bufferedReader.readLine();
            if (response.matches("y")){
                TClient.bufferedWriter.write("Start "+account.getUsername()+" Peer2Peer Server");
                TClient.bufferedWriter.newLine();
                TClient.bufferedWriter.flush();
                sendMessage(userName + " accepted your private messaging request");
                sendMessage("Client-Info: " + TClient.socket.getPort() );
                // System.out.println("target port = " + TClient.socket.getPort());
                // System.out.println("Sender port = " + this.socket.getPort());
                // TClient.bufferedWriter.write("Client-Info: " + this.socket.getPort());
                // TClient.bufferedWriter.newLine();
                // TClient.bufferedWriter.flush();
            }
            else{
                sendMessage(userName + " has declined your private messaging request");
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
        account.setActiveClient(null);
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
