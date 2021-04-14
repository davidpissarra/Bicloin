package pt.tecnico.rec.domain;

import java.util.HashMap;
import java.util.Map;

public class Records {
    private Map<String, Integer> balances = new HashMap<>();
    private Map<String, Integer> bikes = new HashMap<>();
    private Map<String, Integer> bikeUpStats = new HashMap<>();
    private Map<String, Integer> bikeDownStats = new HashMap<>();

    public Integer readBalance(String registerName) {
        if(!balances.containsKey(registerName)) {
            writeBalance(registerName, 0);
        }
        return balances.get(registerName);
    }
    
    public void writeBalance(String registerName, Integer balance) {
        balances.put(registerName, balance);
    }

    public Integer readBikes(String registerName) {
        return bikes.get(registerName);
    }

    public void writeBikes(String registerName, Integer bikes) {
        this.bikes.put(registerName, bikes);
    }

    public Integer readBikeUpStats(String registerName) {
        return bikeUpStats.get(registerName);
    }

    public void writeBikeUpStats(String registerName, Integer bikeUpStats) {
        this.bikeUpStats.put(registerName, bikeUpStats);
    }
    
    public Integer readBikeDownStats(String registerName) {
        return bikeDownStats.get(registerName);
    }

    public void writeBikeDownStats(String registerName, Integer bikeDownStats) {
        this.bikeDownStats.put(registerName, bikeDownStats);
    }
}
