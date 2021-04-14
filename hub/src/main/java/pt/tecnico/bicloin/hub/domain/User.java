package pt.tecnico.bicloin.hub.domain;

import pt.tecnico.bicloin.hub.domain.exception.InvalidUserException;

import static pt.tecnico.bicloin.hub.domain.exception.ErrorMessage.*;

public class User {

    private String id;
    private String name;
    private String phoneNumber;
    
    public User(String id, String name, String phoneNumber){
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if(!(isAlphaNumeric(id) && id.length() >= 3 && id.length() <= 30)) {
            throw new InvalidUserException(INVALID_USER_ID, id);
        }
        this.id = id;
    }

    public void setPhoneNumber(String phoneNumber) {
        if(!(isNumeric(phoneNumber) && phoneNumber.startsWith("+"))) {
            throw new InvalidUserException(INVALID_PHONE_NUMBER, this.id);
        }
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setName(String name) {
        if(name.length() > 30) {
            throw new InvalidUserException(INVALID_NAME, this.id);
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private boolean isNumeric(String s) {
        return s != null && s.matches("^[0-9+]*$");
    }

    private boolean isAlphaNumeric(String s) {
        return s != null && s.matches("^[a-zA-Z0-9]*$");
    }

}
