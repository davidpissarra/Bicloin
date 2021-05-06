package pt.tecnico.rec;

import java.io.IOException;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class RecTester {
	
	public static void main(String[] args) throws ZKNamingException, IOException, InterruptedException {
		System.out.println(RecTester.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String path = args[2];
		
		try {
			ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
			RecFrontend frontend = new RecFrontend(zkNaming);
			ZKRecord record = zkNaming.lookup("/grpc/bicloin/rec/1");
			String output = frontend.ping(record);
			System.out.println(output);
			frontend.close();
		} catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " +
					e.getStatus().getDescription());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
}