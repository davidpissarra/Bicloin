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

		if (args.length < 7 && args.length > 8) {
			System.err.println("Argumentos inválidos!");
			return;
		}
		
		System.out.printf("Recebidos %d argumentos%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String host = args[2];
		final String port = args[3];
		final Integer instance = Integer.valueOf(args[4]);
		final String users = args[5];
		final String stations = args[6];

		final String path = "/grpc/bicloin/hub/" + instance;

		try {
			ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
			zkNaming.rebind(path, host, port);

			RecFrontend recFrontend = new RecFrontend(zkNaming);

			boolean initRec = args.length == 8;
			final BindableService impl = new HubServerImpl(instance, recFrontend, users, stations, initRec);

			// Create a new server to listen on port
			Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

			// Start the server
			server.start();

			// Server threads are running in the background.
			final String target = host + ":" + port;
			System.out.printf("Instância %d do Hub a começar em %s.%n", instance, target);


			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();

		} catch(InvalidUserException | InvalidStationException | FileNotFoundException e) {
			System.out.println(e.getMessage());
			return;
		} catch (ZKNamingException e) {
			System.out.println("ERRO Conexão ao hub falhada.");
			return;
		}
	}
	
}
