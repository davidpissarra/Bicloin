package pt.tecnico.rec;

import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.Rec.*;

public class RecTester {
	
	public static void main(String[] args) {
		System.out.println(RecTester.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		String zooHost = args[0];
		String zooPort = args[1];
		String path = args[2];

		try (RecFrontend frontend = new RecFrontend(zooHost, zooPort, path)) {
			PingRequest pingRequest = PingRequest.newBuilder().build();
			PingResponse pingResponse = frontend.ping(pingRequest);
			System.out.println(pingResponse);



		} catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
		} catch(Exception e) {
			System.out.println("Caught exception with description: " + e.getMessage());
		}
	}
	
}
