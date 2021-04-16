package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopUpIT extends BaseIT {

    @Test
	public void topUp() {
		String output = frontend.topUp(10);
		String[] tokens = output.split(" ");
        String name = tokens[0];
        Integer balance = Integer.parseInt(tokens[1]);
        String currency = tokens[2];
		assertEquals("alice", name);
        assertEquals("BIC", currency);
        assertEquals(true, balance >= 0);
	}
    
}
