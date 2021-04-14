package pt.tecnico.bicloin.hub.domain.exception;

public enum ErrorMessage {
    
    INVALID_USER_ID("Username % é inválido."),
    INVALID_PHONE_NUMBER("Número de utilizador inválido para o utilizador %s"),
    INVALID_NAME("Nome inválido para o utilizador %s"),
    INVALID_STATION_ABBREVIATION("Abreviação %s é inválida."),
    INVALID_STATION_LATITUDE("Latitude inválida para a estação %s."),
    INVALID_STATION_LONGITUDE("Longitude inválida para a estação %s."),
    INVALID_NUMBER_BIKES_AVAILABLE("A estação %s não pode ter o número de bicicletas disponíveis superior ao número de docas.");

    public final String label;

    ErrorMessage(String label) {
        this.label = label;
    }
    
}
