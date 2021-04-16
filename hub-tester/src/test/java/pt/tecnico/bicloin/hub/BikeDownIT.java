package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BikeDownIT extends BaseIT {

    private final static String ABREV_ISTT = "istt";
    
    @Test
	public void bikeDownSuccessful() {
        frontend.topUp(10);
        frontend.move((float) 38.7376, (float) -9.3031); // tagus
		frontend.bikeUp(ABREV_ISTT);
        String output = frontend.bikeDown(ABREV_ISTT);
		assertEquals("OK", output);
	}

    @Test
	public void bikeDownOutOfRange() {
        frontend.topUp(10);
        frontend.move((float) 38.7376, (float) -9.3031); // tagus
		frontend.bikeUp(ABREV_ISTT);
        frontend.move((float) -38.7376, (float) 9.3031); // far away
		String output = frontend.bikeDown(ABREV_ISTT);
		assertEquals("ERRO fora de alcance.", output);
        frontend.move((float) 38.7376, (float) -9.3031); // tagus
        frontend.bikeDown(ABREV_ISTT);
	}

    @Test
	public void notBikedUp() {
        frontend.move((float) 38.7376, (float) -9.3031); // tagus
        String output = frontend.bikeDown(ABREV_ISTT);
		assertEquals("ERRO utilizador não tem uma bicicleta levantada.", output);
	}

}