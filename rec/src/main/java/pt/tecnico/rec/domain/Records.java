package pt.tecnico.rec.domain;

import java.util.HashMap;
import java.util.Map;

public class Records {
    private Map<String, Integer> balances = new HashMap<>();
    private Map<String, Integer> stationBikes = new HashMap<>();

    public Integer readBalance(String registerName) {
        if(!balances.containsKey(registerName)) {
            writeBalance(registerName, 0);
        }
        return balances.get(registerName);
    }
    
    public void writeBalance(String registerName, Integer balance) {
        balances.put(registerName, balance);
    }

    public Integer readStationBikes(String registerName) {
        return stationBikes.get(registerName);
    }

    public void writeStationBikes(String registerName, Integer nBikes) {
        stationBikes.put(registerName, nBikes);
    }
}
