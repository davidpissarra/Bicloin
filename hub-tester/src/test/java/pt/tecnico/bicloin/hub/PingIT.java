package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingIT extends BaseIT {
    
    @Test
	public void ping() {
		String output = frontend.ping();
		assertEquals("Servidor hub está ligado.", output);
	}

}
