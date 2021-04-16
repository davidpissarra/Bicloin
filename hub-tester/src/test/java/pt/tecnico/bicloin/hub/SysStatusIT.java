package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SysStatusIT extends BaseIT {
    
    @Test
	public void ping() {
		String output = frontend.sysStatus();
		assertEquals("Hub instance number 1 is UP.\nRec instance number 1 is UP.\n", output);
	}

}
