package pt.tecnico.bicloin.app;

import java.io.InputStream;
import java.util.Scanner;

import com.google.protobuf.Message;

import pt.tecnico.bicloin.hub.grpc.Hub.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class AppMain {
	
	public static void main(String[] args) throws ZKNamingException {
		System.out.println(AppMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if(args.length < 6 || args.length > 7){
			System.err.println("Invalid arguments!");
			return;
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String userId = args[2];
		final String userPhoneNumber = args[3];
		final String userLatitude = args[4];
		final String userLongitude = args[5];

		App app = new App(zooHost, zooPort);

		if(args.length == 6) {
			readCommands(System.in, app);
		}

		if(args.length == 7) {
			// TODO readCommands();
		}
	}

	private static void readCommands(InputStream inputStream, App app) {
		try (Scanner scanner = new Scanner(inputStream)) {
			while(true) {
				String command = scanner.nextLine();
	
				if(command.equals("ping")) {
					printResponse( app.ping() );
				}

				else if(command.equals("sys_status")) {
					printResponse( app.sysStatus() );
				}

			}
		}
	}

	private static void printResponse(Message message) {
		if(message instanceof PingResponse) {
			PingResponse pingResponse = (PingResponse) message;
			System.out.println(pingResponse.getOutput());
		} else if(message instanceof SysStatusResponse) {
			SysStatusResponse sysStatusResponse = (SysStatusResponse) message;
			System.out.println(sysStatusResponse.getOutput());
		} 
	}

}
