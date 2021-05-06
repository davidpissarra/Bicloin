package pt.tecnico.rec;

import org.junit.jupiter.api.*;

import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingIT extends BaseIT {
	
	@Test
	public void ping() {
		ZKRecord record = frontend.getRecords().iterator().next();
		String output = frontend.ping(record);
		assertEquals("Réplica 1 do rec está ligada.", output);
	}

}