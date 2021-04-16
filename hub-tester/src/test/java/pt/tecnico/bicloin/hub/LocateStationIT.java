package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocateStationIT extends BaseIT {
    
    private final static Integer N_STATIONS = 3;

    private final static String EXPECTED = "istt, lat 38.7372, -9.3023 long, 20 docas, 4 BIC prémio, 12 bicicletas, a 218 metros\n" +
                            "stao, lat 38.6867, -9.3124 long, 30 docas, 3 BIC prémio, 20 bicicletas, a 5804 metros\n" +
                            "jero, lat 38.6972, -9.2064 long, 30 docas, 3 BIC prémio, 20 bicicletas, a 9301 metros";

    @Test
	public void locateStation() {
        frontend.move((float) 38.7380, (float) -9.3000);
		String output = frontend.scan(N_STATIONS);
		assertEquals(EXPECTED, output);
	}
}
