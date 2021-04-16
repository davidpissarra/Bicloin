package pt.tecnico.rec;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingIT extends BaseIT {
	
	@Test
	public void ping() {
		String output = frontend.ping();
		assertEquals("Rec instance number 1 is UP.", output);
	}

}