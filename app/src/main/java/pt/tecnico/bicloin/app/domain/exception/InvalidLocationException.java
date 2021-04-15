package pt.tecnico.bicloin.app.domain.exception;

public class InvalidLocationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorMessage errorMessage;

    public InvalidLocationException(ErrorMessage errorMessage) {
        super(errorMessage.label);
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
    
}