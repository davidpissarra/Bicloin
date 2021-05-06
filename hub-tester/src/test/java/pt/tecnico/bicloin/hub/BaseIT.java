package pt.tecnico.bicloin.hub;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.*;

import pt.tecnico.bicloin.hub.domain.AppUser;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;


public class BaseIT {

	private static final String TEST_PROP_FILE = "/test.properties";
	protected static Properties testProps;

	static HubFrontend frontend;
	
	@BeforeAll
	public static void oneTimeSetup () throws IOException {
		testProps = new Properties();
		
		try {
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Test properties:");
			System.out.println(testProps);

			String zooHost = testProps.getProperty("zoo.host");
			String zooPort = testProps.getProperty("zoo.port");
			String username = testProps.getProperty("user.id");
			String phoneNumber = testProps.getProperty("user.phoneNumber");
			Float latitude = Float.parseFloat(testProps.getProperty("user.latitude"));
			Float longitude = Float.parseFloat(testProps.getProperty("user.longitude"));

			frontend = new HubFrontend(new ZKNaming(zooHost, zooPort)
										, new AppUser(username, phoneNumber, latitude, longitude)
										, "/grpc/bicloin/hub/1");

		} catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		} catch (ZKNamingException e) {
			System.out.print(e.getMessage());
		}
	}
	
	@AfterAll
	public static void cleanup() throws Exception {
		frontend.close();
	}

}
