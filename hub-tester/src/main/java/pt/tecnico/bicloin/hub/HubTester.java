package pt.tecnico.bicloin.hub;

import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.domain.AppUser;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class HubTester {
	
	public static void main(String[] args) {
		System.out.println(HubTester.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		String zooHost = args[0];
		String zooPort = args[1];
		AppUser user = new AppUser(args[2], args[3], Float.valueOf(args[4]), Float.valueOf(args[5]));

		try {
			ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
			HubFrontend frontend = new HubFrontend(zkNaming, user, "/grpc/bicloin/hub/1");
			String pingOutput = frontend.ping();
			System.out.println(pingOutput);
			frontend.close();

		} catch (StatusRuntimeException e) {
			System.out.println("Caught Status Runtime exception with description: " + e.getStatus());
		} catch(ZKNamingException e) {
			System.out.println("Caught ZKNaming exception with description: " + e.getMessage());
		} catch(Exception e) {
			System.out.println("Caught exception with description: " + e.getMessage());
		}
	}
	
}
