package pt.tecnico.rec;

public class RecordMain {
	
	public static void main(String[] args) {
		System.out.println(RecordMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments TODO insufficient  parsing
		if (args.length < 5) {
		System.err.println("Argument(s) missing!");
		//System.err.printf("Usage: java %s port%n", TTTServer.class.getName());
		return;
		}

		final int port = Integer.parseInt(args[3]);
		final BindableService impl = new RecordServiceImpl();

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		// Start the server
		server.start();

		// Server threads are running in the background.
		System.out.println("Server started");




		final String target = "localhost" + ":" + port;
		// Channel is the abstraction to connect to a service endpoint.
		// Let us use plaintext communication because we do not have certificates.
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		// It is up to the client to determine whether to block the call.
		// Here we create a blocking stub, but an async stub,
		// or an async stub with Future are always possible.
		RecordServiceGrpc.RecordServiceBlockingStub stub = RecordServiceGrpc.newBlockingStub(channel);

		String ping = stub.ctrlPing(PingRequest);
		System.out.println(ping);






		// Do not exit the main thread. Wait until server is terminated.
		server.awaitTermination();

		// TODO NAME SERVER
	}	
}
