package pt.tecnico.bicloin.app;

import java.io.InputStream;
import java.util.Scanner;

import com.google.protobuf.Message;

import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.grpc.Hub.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import pt.tecnico.bicloin.app.domain.User;
import pt.tecnico.bicloin.app.domain.exception.InvalidUserException;

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
		try {
			
			App app = new App(zooHost, zooPort, new User(args[2], args[3], Float.valueOf(args[4]), Float.valueOf(args[5])));
			
			if(args.length == 6) {
				readCommands(System.in, app);
			}
			
			if(args.length == 7) {
				// TODO readCommands();
			}
		} catch(InvalidUserException e) {
			System.out.println(e.getMessage());
			return;
		}
	}

	private static void readCommands(InputStream inputStream, App app) {
		try (Scanner scanner = new Scanner(inputStream)) {
			while(true) {
				String command = scanner.nextLine();

				if(command.equals("ping")) {
					app.ping();
				}
				
				else if(command.equals("sys-status")) {
					app.sysStatus();
				}

				else if(command.equals("balance")) {
					app.balance();
				}

				else if(command.contains("top-up")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					Integer value = Integer.parseInt(tokens[1]);
					if(value >= 1 && value <= 20) {
						app.topUp(value);
					}
					else {
						System.out.println("Carregamento deve ser entre 1 e 20 Euros.");
					}
				}
				else if(command.contains("info")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					String abrev = tokens[1];
					app.infoStation(abrev);
				}
				else if(command.contains("tag")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 4) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					float latitude = Float.parseFloat(tokens[1]);
					float longitude = Float.parseFloat(tokens[2]);
					String tagName = tokens[3];
					app.tag(latitude, longitude, tagName);
				}
				else if(command.contains("move")) {
					String[] tokens = command.split(" ");
					if(tokens.length == 2) {
						String tagName = tokens[1];
						app.move(tagName);
					}
					else if(tokens.length == 3){
						float latitude = Float.parseFloat(tokens[1]);
						float longitude = Float.parseFloat(tokens[2]);
						app.move(latitude, longitude);
					}
					else {
						System.out.println("Comando não encontrado.");
						continue;
					}
				}
				else if(command.equals("at")) {
					app.at();
				}
				else if(command.contains("scan")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					Integer nStations = Integer.parseInt(tokens[1]);
					app.scan(nStations);
				}
				else if(command.contains("bike-up")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					String abrev = tokens[1];
					app.bikeUp(abrev);
				}
				else if(command.contains("bike-down")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					String abrev = tokens[1];
					app.bikeDown(abrev);
				}
				else {
					System.out.println("Comando não encontrado.");
				}
			}
		}
	}

}
