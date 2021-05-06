package pt.tecnico.rec.domain;

import java.util.HashMap;
import java.util.Map;

public class Records {
    private Map<String, RecBalance> balances = new HashMap<>();
    private Map<String, RecBikes> bikes = new HashMap<>();
    private Map<String, RecBikeUpStats> bikeUpStats = new HashMap<>();
    private Map<String, RecBikeDownStats> bikeDownStats = new HashMap<>();
    private Map<String, RecIsUserBikedUp> isUserBikedUp = new HashMap<>();

    public synchronized RecBalance readBalance(String registerName) {
        if(!balances.containsKey(registerName)) {
            RecBalance recBalance = new RecBalance(0, 0);
            writeBalance(registerName, recBalance);
        }
        return balances.get(registerName);
    }
    
    public synchronized void writeBalance(String registerName, RecBalance balance) {
        balances.put(registerName, balance);
    }

    public synchronized RecBikes readBikes(String registerName) {
        return bikes.get(registerName);
    }

    public synchronized void writeBikes(String registerName, RecBikes bikes) {
        this.bikes.put(registerName, bikes);
    }

    public synchronized RecBikeUpStats readBikeUpStats(String registerName) {
        return bikeUpStats.get(registerName);
    }

    public synchronized void writeBikeUpStats(String registerName, RecBikeUpStats bikeUpStats) {
        this.bikeUpStats.put(registerName, bikeUpStats);
    }
    
    public synchronized RecBikeDownStats readBikeDownStats(String registerName) {
        return bikeDownStats.get(registerName);
    }

    public synchronized void writeBikeDownStats(String registerName, RecBikeDownStats bikeDownStats) {
        this.bikeDownStats.put(registerName, bikeDownStats);
    }

    public synchronized RecIsUserBikedUp readIsUserBikedUp(String registerName) {
        return isUserBikedUp.get(registerName);
    }

    public synchronized void writeIsUserBikedUp(String registerName, RecIsUserBikedUp isBikedUp) {
        this.isUserBikedUp.put(registerName, isBikedUp);
    }

    public synchronized void cleanRecords() {
        balances.clear();
        bikes.clear();
        bikeUpStats.clear();
        bikeDownStats.clear();
        isUserBikedUp.clear();
    }
}
