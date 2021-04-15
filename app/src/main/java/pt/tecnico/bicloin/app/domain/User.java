package pt.tecnico.bicloin.app.domain;

import pt.tecnico.bicloin.app.domain.exception.InvalidUserException;

import static pt.tecnico.bicloin.app.domain.exception.ErrorMessage.*;

public class User {

    private String id;
    private String phoneNumber;
    private float latitude;
    private float longitude;
    
    public User(String id, String phoneNumber, float latitude, float longitude){
        setId(id);
        setPhoneNumber(phoneNumber);
        setLocation(latitude, longitude);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if(isAlphaNumeric(id) && id.length() >= 3 && id.length() <= 30) {
            this.id = id;
        }
        else {
            throw new InvalidUserException(INVALID_USER_ID);
        }
    }

    public void setPhoneNumber(String phoneNumber) {
        if(isNumeric(phoneNumber) && phoneNumber.startsWith("+")) {
            this.phoneNumber = phoneNumber;
        }
        else {
            throw new InvalidUserException(INVALID_PHONE_NUMBER);
        }
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLocation(float latitude, float longitude) {
        if(!(latitude >= -90 && latitude <= 90)) {
            throw new InvalidUserException(INVALID_LATITUDE);
        }
        if(!(longitude >= -90 && longitude <= 90)) {
            throw new InvalidUserException(INVALID_LONGITUDE);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private boolean isNumeric(String s) {
        return s != null && s.matches("^[0-9+]*$");
    }

    private boolean isAlphaNumeric(String s) {
        return s != null && s.matches("^[a-zA-Z0-9]*$");
    }

}
