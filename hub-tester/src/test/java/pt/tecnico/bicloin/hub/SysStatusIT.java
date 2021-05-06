package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SysStatusIT extends BaseIT {
    
    @Test
	public void ping() {
		String output = frontend.sysStatus();
		assertEquals("Servidor hub está ligado.\n"
						+ "Réplica 1 do rec está ligada.\n"
						+ "Réplica 2 do rec está ligada.\n"
						+ "Réplica 3 do rec está ligada.\n"
							, output);
	}

}
