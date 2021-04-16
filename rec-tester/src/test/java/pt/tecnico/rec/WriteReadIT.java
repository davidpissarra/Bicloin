package pt.tecnico.rec;

import org.junit.jupiter.api.*;

import io.grpc.StatusRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.InvalidProtocolBufferException;

public class WriteReadIT extends BaseIT {
    
    protected static String BALANCE_REGISTER_NAME = "balance-anibal";
	protected static Integer BALANCE = 5;

	protected static String BIKES_REGISTER_NAME = "bikes-ist";
	protected static Integer BIKES = 15;

	protected static String BIKE_UP_STATS_REGISTER_NAME = "bikeUpStats-ist";
	protected static Integer BIKE_UP_STATS = 0;

	protected static String BIKE_DOWN_STATS_REGISTER_NAME = "bikeDownStats-ist";
	protected static Integer BIKE_DOWN_STATS = 0;

	protected static String IS_BIKED_UP_REGISTER_NAME = "isBikedUp-anibal";
	protected static Boolean IS_BIKED_UP = false;

	// clean-up for each test
	
	@AfterEach
	public void tearDown() {
		try {
			frontend.clean();
		} catch (StatusRuntimeException | InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}
		
	// tests 
	
	@Test
	public void write() {
		try {
			frontend.writeBalance(BALANCE_REGISTER_NAME, BALANCE);
			frontend.writeBikes(BIKES_REGISTER_NAME, BIKES);
			frontend.writeBikeUpStats(BIKE_UP_STATS_REGISTER_NAME, BIKE_UP_STATS);
			frontend.writeBikeDownStats(BIKE_DOWN_STATS_REGISTER_NAME, BIKE_DOWN_STATS);
			frontend.writeIsBikedUp(IS_BIKED_UP_REGISTER_NAME, IS_BIKED_UP);

			Integer balance = frontend.readBalance(BALANCE_REGISTER_NAME);
			Integer bikes = frontend.readBikes(BIKES_REGISTER_NAME);
			Integer bikeUpStats = frontend.readBikeUpStats(BIKE_UP_STATS_REGISTER_NAME);
			Integer bikeDownStats = frontend.readBikeDownStats(BIKE_DOWN_STATS_REGISTER_NAME);
			Boolean isBikedUp = frontend.readIsBikedUp(IS_BIKED_UP_REGISTER_NAME);

			assertEquals(BALANCE, balance);
			assertEquals(BIKES, bikes);
			assertEquals(BIKE_UP_STATS, bikeUpStats);
			assertEquals(BIKE_DOWN_STATS, bikeDownStats);
			assertEquals(IS_BIKED_UP, isBikedUp);
		} catch (StatusRuntimeException | InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

}
