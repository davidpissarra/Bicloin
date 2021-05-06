package pt.tecnico.rec;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.*;

import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class BaseIT {

	private static final String TEST_PROP_FILE = "/test.properties";
	protected static Properties testProps;

	static RecFrontend frontend;
	protected static String recPath;
	
	@BeforeAll
	public static void oneTimeSetup () throws IOException, ZKNamingException {
		testProps = new Properties();
		
		try {
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Test properties:");
			System.out.println(testProps);
			
			String zooHost = testProps.getProperty("zoo.host");
			String zooPort = testProps.getProperty("zoo.port");
			recPath = testProps.getProperty("rec.path");
			frontend = new RecFrontend(new ZKNaming(zooHost, zooPort));

		} catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		}

	}
	
	@AfterAll
	public static void cleanup() {
		
	}

}
