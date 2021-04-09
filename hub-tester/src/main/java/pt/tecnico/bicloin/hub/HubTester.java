package pt.tecnico.bicloin.hub;

import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.grpc.Hub.*;
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
		String path = args[2];

		try (HubFrontend frontend = new HubFrontend(zooHost, zooPort, path)) {
			SysStatusRequest sysStatusRequest = SysStatusRequest.newBuilder().build();
			SysStatusResponse sysStatusResponse = frontend.sysStatus(sysStatusRequest);
			System.out.println(sysStatusResponse.getOutput());

		} catch (StatusRuntimeException e) {
			System.out.println("Caught Status Runtime exception with description: " + e.getStatus());
		} catch(ZKNamingException e) {
			System.out.println("Caught ZKNaming exception with description: " + e.getMessage());
		} catch(Exception e) {
			System.out.println("Caught exception with description: " + e.getMessage());
		}
	}
	
}
