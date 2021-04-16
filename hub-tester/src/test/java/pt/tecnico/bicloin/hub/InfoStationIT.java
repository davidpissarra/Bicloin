package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InfoStationIT extends BaseIT {
    
    private final static String ABREV_1 = "ista";
    private final static String EXPECTED_1_PART_1 = "IST Alameda, lat 38.7369, -9.1366 long, 20 docas, 3 BIC prémio, ";
    private final static String EXPECTED_1_LINK = "https://www.google.com/maps/place/38.7369,-9.1366";

    private final static String ABREV_2 = "istt";
    private final static String EXPECTED_2_PART_1 = "IST Taguspark, lat 38.7372, -9.3023 long, 20 docas, 4 BIC prémio, ";
    private final static String EXPECTED_2_LINK = "https://www.google.com/maps/place/38.7372,-9.3023";
    
    @Test
	public void infoStation() {
        frontend.move((float) 38.7380, (float) -9.3000);
		String output = frontend.infoStation(ABREV_1);
        String outputPart1 = output.substring(0, 64);
		assertEquals(EXPECTED_1_PART_1, outputPart1);
        
        String[] tokens = output.substring(64).split(" ");

        Integer bikes = Integer.parseInt(tokens[0]);
        assertEquals(true, bikes >= 0);
        assertEquals(tokens[1], "bicicletas,");

        Integer bikeUpStats = Integer.parseInt(tokens[2]);
        assertEquals(true, bikeUpStats >= 0);
        assertEquals(tokens[3], "levantamentos,");

        Integer bikeDownStats = Integer.parseInt(tokens[4]);
        assertEquals(true, bikeDownStats >= 0);
        assertEquals(tokens[5], "devoluções,");

        assertEquals(EXPECTED_1_LINK, tokens[6]);


        // second

        output = frontend.infoStation(ABREV_2);
        String outputPart2 = output.substring(0, 66);
        assertEquals(EXPECTED_2_PART_1, outputPart2);
        tokens = output.substring(66).split(" ");
        
        bikes = Integer.parseInt(tokens[0]);
        assertEquals(true, bikes >= 0);
        assertEquals(tokens[1], "bicicletas,");

        bikeUpStats = Integer.parseInt(tokens[2]);
        assertEquals(true, bikeUpStats >= 0);
        assertEquals(tokens[3], "levantamentos,");

        bikeDownStats = Integer.parseInt(tokens[4]);
        assertEquals(true, bikeDownStats >= 0);
        assertEquals(tokens[5], "devoluções,");

        assertEquals(EXPECTED_2_LINK, tokens[6]);
	}

}
