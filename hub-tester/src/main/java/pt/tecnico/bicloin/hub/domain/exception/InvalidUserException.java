package pt.tecnico.bicloin.hub.domain.exception;

public class InvalidUserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorMessage errorMessage;

    public InvalidUserException(ErrorMessage errorMessage) {
        super(errorMessage.label);
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
    
}