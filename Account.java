import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;

public class Account {
    // Attributes
    private String username, password;
    private boolean loggedIn;
    private LocalTime lockedOutFinishTime;
    private List<Account> blockedAccounts;

    // Constructor
    public Account(String username, String password){
        this.username = username;
        this.password = password;
        this.loggedIn = false;
        this.blockedAccounts = new ArrayList<>();
        this.lockedOutFinishTime = LocalTime.now();
    }


    // Getter and Setters

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    public boolean getLoggedIn() {
        return this.loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public LocalTime getLockedOutFinishTime() {
        return this.lockedOutFinishTime;
    }

    public void setLockedOutFinishTime(LocalTime lockedOutFinishTime) {
        this.lockedOutFinishTime = lockedOutFinishTime;
    }
    
    public List<Account> getBlockedAccounts() {
        return this.blockedAccounts;
    }

    public void setBlockedAccounts(List<Account> blockedAccounts) {
        this.blockedAccounts = blockedAccounts;
    }

}