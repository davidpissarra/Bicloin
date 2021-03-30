package pt.tecnico.rec;

import java.util.Hash;
import java.util.HashMap;

public class Records {
    
    private Map<String, Integer> userBalances = new HashMap<>();

    public Records() { }

    public Records(Map<String, UserRecord> userRecords) {
        this.userRecords = userRecords;
    }

    public Integer readBalance(String username) {
        if(userBalances.get(username) == null) {
            writeBalance(username, 0);
        }
        return userBalances.get(username);
    }

    public void writeBalance(String username, Integer amount) {
        userBalances.put(username, amount);
    }
}
