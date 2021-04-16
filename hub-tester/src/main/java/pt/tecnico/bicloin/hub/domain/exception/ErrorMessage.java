package pt.tecnico.bicloin.hub.domain.exception;

public enum ErrorMessage {
    
    INVALID_USER_ID("Username inválido."),
    INVALID_PHONE_NUMBER("Número de utilizador inválido para o utilizador %s."),
    INVALID_LATITUDE("Latitude inválida."),
    INVALID_LONGITUDE("Longitude inválida.");

    public final String label;

    ErrorMessage(String label) {
        this.label = label;
    }
    
}
