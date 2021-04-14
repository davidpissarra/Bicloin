package pt.tecnico.bicloin.hub.domain.exception;

public class InvalidStationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private final ErrorMessage errorMessage;

    public InvalidStationException(ErrorMessage errorMessage) {
        super(errorMessage.label);
        this.errorMessage = errorMessage;
    }

    public InvalidStationException(ErrorMessage errorMessage, String value) {
        super(String.format(errorMessage.label, value));
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
    
}