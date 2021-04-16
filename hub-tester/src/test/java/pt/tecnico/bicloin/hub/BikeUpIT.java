package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BikeUpIT extends BaseIT {
    
    private final static String ABREV_ISTT = "istt";

    @Test
	public void bikeUpSuccessful() {
        frontend.topUp(10);
        frontend.move((float) 38.7376, (float) -9.3031); // tagus
		String output = frontend.bikeUp(ABREV_ISTT);
		assertEquals("OK", output);
        frontend.bikeDown(ABREV_ISTT);
	}
    
    @Test
	public void bikeUpOutOfRange() {
        frontend.topUp(10);
        frontend.move((float) -38.7376, (float) 9.3031); // far away
		String output = frontend.bikeUp(ABREV_ISTT);
		assertEquals("ERRO fora de alcance.", output);
	}

    @Test
	public void alreadyBikeUp() {
        frontend.topUp(10);
        frontend.move((float) 38.7376, (float) -9.3031); // tagus
		String output = frontend.bikeUp(ABREV_ISTT);
        output = frontend.bikeUp(ABREV_ISTT);
		assertEquals("ERRO utilizador já tem uma bicicleta levantada.", output);
        frontend.bikeDown(ABREV_ISTT);
	}

}
