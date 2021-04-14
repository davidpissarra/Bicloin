package pt.tecnico.bicloin.hub.domain;

import static pt.tecnico.bicloin.hub.domain.exception.ErrorMessage.*;

import pt.tecnico.bicloin.hub.domain.exception.InvalidStationException;

public class Station {
    private String name;
    private String abrev;
    private float latitude;
    private float longitude;
    private Integer docks;
    private Integer reward;

    public Station(String name, String abrev, float latitude, float longitude, Integer docks, Integer reward){
        setAbrev(abrev);
        setLocation(latitude, longitude);
        this.name = name;
        this.docks = docks;
        this.reward = reward;
    }
    
    public final String getName() {
        return name;
    }

    public final String getAbrev() {
        return abrev;
    }

    public final float getLatitude() {
        return latitude;
    }

    public final float getLongitude() {
        return longitude;
    }

    public final Integer getDocks() {
        return docks;
    }
    
    public final Integer getReward() {
        return reward;
    }

    public void setLocation(float latitude, float longitude) {
        if(!(latitude <= 90 && latitude >= -90)) {
            throw new InvalidStationException(INVALID_STATION_LATITUDE, this.abrev);
        }
        if(!(longitude <= 90 && longitude >= -90)) {
            throw new InvalidStationException(INVALID_STATION_LONGITUDE, this.abrev);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public void setAbrev(String abrev) {
        if(!abrev.matches("[a-zA-Z0-9]{4}")) {
            throw new InvalidStationException(INVALID_STATION_ABBREVIATION, abrev);
        }
        this.abrev = abrev;
    }

}
