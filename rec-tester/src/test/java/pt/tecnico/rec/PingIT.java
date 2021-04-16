package pt.tecnico.rec;

import org.junit.jupiter.api.*;

import pt.tecnico.rec.grpc.Rec.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingIT extends BaseIT {
	
	// tests 
	
	@Test
	public void ping() {
		String output = frontend.ping();
		assertEquals("Rec instance number 1 is UP.", output);
	}

}