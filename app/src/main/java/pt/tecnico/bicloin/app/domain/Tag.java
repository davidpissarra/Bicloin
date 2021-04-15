package pt.tecnico.bicloin.app.domain;

import pt.tecnico.bicloin.app.domain.exception.InvalidLocationException;

import static pt.tecnico.bicloin.app.domain.exception.ErrorMessage.*;

public class Tag {

    private float latitude;
    private float longitude;

    public Tag(float latitude, float longitude) {
        setLocation(latitude, longitude);
    }

    
    public float getLatitude() {
        return this.latitude;
    }

    public float getLongitude() {
        return this.longitude;
    }

    public void setLocation(float latitude, float longitude) {
        if(!(latitude >= -90 && latitude <= 90)) {
            throw new InvalidLocationException(INVALID_LATITUDE);
        }
        if(!(longitude >= -90 && longitude <= 90)) {
            throw new InvalidLocationException(INVALID_LONGITUDE);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
