package pt.tecnico.rec;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;

public class RecMain {
	
	public static void main(String[] args) throws IOException, InterruptedException, ZKNamingException {
		System.out.println(RecMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 5) {
			System.err.println("Argument(s) missing!");
			return;
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String host = args[2];
		final String port = args[3];
		final Integer instance = Integer.valueOf(args[4]);

		final String path = "/grpc/bicloin/rec/" + instance;

		ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
		zkNaming.rebind(path, host, port);

		final BindableService impl = new RecServerImpl(instance);

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

		// Start the server
		server.start();

		// Server threads are running in the background.
		System.out.println("Server started");

		// Do not exit the main thread. Wait until server is terminated.
		server.awaitTermination();

		zkNaming.unbind(path, host, port);
	}	
}
