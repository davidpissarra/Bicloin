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
			User user = new User(args[2], args[3], Float.valueOf(args[4]), Float.valueOf(args[5]));
			
			App app = new App(zooHost, zooPort);
			
			if(args.length == 6) {
				readCommands(System.in, app, user);
			}
			
			if(args.length == 7) {
				// TODO readCommands();
			}
		} catch(InvalidUserException e) {
			System.out.println(e.getMessage());
			return;
		}
	}

	private static void readCommands(InputStream inputStream, App app, User user) {
		try (Scanner scanner = new Scanner(inputStream)) {
			while(true) {
				String command = scanner.nextLine();

				if(command.equals("ping")) {
					printResponse( app.ping(), user );
				}
				
				else if(command.equals("sys-status")) {
					printResponse( app.sysStatus(), user );
				}

				else if(command.equals("balance")) {
					printResponse( app.balance(user), user );
				}

				else if(command.contains("top-up")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					Integer value = Integer.parseInt(tokens[1]);
					if(value >= 1 && value <= 20) {
						printResponse( app.topUp(value, user), user );
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
					printResponse( app.infoStation(tokens[1]), user );
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
						app.move(tagName, user);
					}
					else if(tokens.length == 3){
						float latitude = Float.parseFloat(tokens[1]);
						float longitude = Float.parseFloat(tokens[2]);
						app.move(latitude, longitude, user);
					}
					else {
						System.out.println("Comando não encontrado.");
						continue;
					}
				}
				else if(command.contains("bike-up")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					//printResponse( app.bikeUp(tokens[1]), user );
				}
				else if(command.contains("bike-down")) {
					String[] tokens = command.split(" ");
					if(tokens.length != 2) {
						System.out.println("Comando não encontrado.");
						continue;
					}
					//printResponse( app.bikeDown(tokens[1]), user );
				}
				else {
					System.out.println("Comando não encontrado.");
				}
			}
		}
	}

	private static void printResponse(Message message, User user) {
		if(message instanceof PingResponse) {
			PingResponse pingResponse = (PingResponse) message;
			System.out.println(pingResponse.getOutput());
		}
		else if(message instanceof SysStatusResponse) {
			SysStatusResponse sysStatusResponse = (SysStatusResponse) message;
			System.out.println(sysStatusResponse.getOutput());
		}
		else if(message instanceof BalanceResponse) {
			BalanceResponse balanceResponse = (BalanceResponse) message;
			System.out.println(user.getId() + " " + balanceResponse.getBalance() + " BIC");
		}
		else if(message instanceof TopUpResponse) {
			TopUpResponse topUpResponse = (TopUpResponse) message;
			System.out.println(user.getId() + " " + topUpResponse.getBalance() + " BIC");
		}
		else if(message instanceof InfoStationResponse) {
			InfoStationResponse infoStationResponse = (InfoStationResponse) message;
			String output = infoStationResponse.getName() + ", "
								+ "lat " + infoStationResponse.getLatitude() + ", "
								+ infoStationResponse.getLongitude() + " long, "
								+ infoStationResponse.getDocks() + " docas, "
								+ infoStationResponse.getReward() + " BIC prémio, "
								+ infoStationResponse.getBikes() + " bicicletas, "
								+ infoStationResponse.getBikeUpStats() + " levantamentos, "
								+ infoStationResponse.getBikeDownStats() + " devoluções, "
								+ "https://www.google.com/maps/place/"
								+ infoStationResponse.getLatitude() + ","
								+ infoStationResponse.getLongitude();
			System.out.println(output);
		}
	}

}
