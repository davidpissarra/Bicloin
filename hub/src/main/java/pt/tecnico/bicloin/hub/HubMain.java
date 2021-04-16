package pt.tecnico.bicloin.hub;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.bicloin.hub.domain.exception.InvalidStationException;
import pt.tecnico.bicloin.hub.domain.exception.InvalidUserException;
import pt.tecnico.rec.RecFrontend;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.FileNotFoundException;
import java.io.IOException;

public class HubMain {
	
	public static void main(String[] args) throws IOException, InterruptedException, ZKNamingException {
		System.out.println(HubMain.class.getSimpleName());
		
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
		final String path = args[4];
		final String users = args[5];
		final String stations = args[6];

		Integer lastSlashIndex = path.lastIndexOf('/');
		final Integer instance = Integer.valueOf(path.substring(lastSlashIndex + 1));

		try {
			ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
			zkNaming.rebind(path, host, port);

			RecFrontend recFrontend = new RecFrontend(zkNaming, "/grpc/bicloin/rec/1");
			HubFrontend hubFrontend = new HubFrontend(zkNaming, null);

			boolean initRec = args.length == 8;
			final BindableService impl = new HubServerImpl(instance, recFrontend, hubFrontend, users, stations, initRec);

			// Create a new server to listen on port
			Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

			// Start the server
			server.start();

			// Server threads are running in the background.
			System.out.println("Server started");

			final String target = "localhost" + ":" + port;

			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();

		} catch(InvalidUserException | InvalidStationException | FileNotFoundException e) {
			System.out.println(e.getMessage());
			return;
		}
	}
	
}
