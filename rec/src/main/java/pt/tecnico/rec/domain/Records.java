package pt.tecnico.rec.domain;

import java.util.HashMap;
import java.util.Map;

public class Records {
    private Map<String, Integer> balances = new HashMap<>();
    private Map<String, Integer> bikes = new HashMap<>();
    private Map<String, Integer> bikeUpStats = new HashMap<>();
    private Map<String, Integer> bikeDownStats = new HashMap<>();
    private Map<String, Boolean> isUserBikedUp = new HashMap<>();

    public synchronized Integer readBalance(String registerName) {
        if(!balances.containsKey(registerName)) {
            writeBalance(registerName, 0);
        }
        return balances.get(registerName);
    }
    
    public synchronized void writeBalance(String registerName, Integer balance) {
        balances.put(registerName, balance);
    }

    public synchronized Integer readBikes(String registerName) {
        return bikes.get(registerName);
    }

    public synchronized void writeBikes(String registerName, Integer bikes) {
        this.bikes.put(registerName, bikes);
    }

    public synchronized Integer readBikeUpStats(String registerName) {
        return bikeUpStats.get(registerName);
    }

    public synchronized void writeBikeUpStats(String registerName, Integer bikeUpStats) {
        this.bikeUpStats.put(registerName, bikeUpStats);
    }
    
    public synchronized Integer readBikeDownStats(String registerName) {
        return bikeDownStats.get(registerName);
    }

    public synchronized void writeBikeDownStats(String registerName, Integer bikeDownStats) {
        this.bikeDownStats.put(registerName, bikeDownStats);
    }

    public synchronized Boolean readIsUserBikedUp(String registerName) {
        return isUserBikedUp.get(registerName);
    }

    public synchronized void writeIsUserBikedUp(String registerName, Boolean isBikedUp) {
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
