package pt.tecnico.bicloin.app;

import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.bicloin.hub.HubFrontend;
import pt.tecnico.bicloin.hub.domain.AppUser;
import pt.tecnico.bicloin.hub.domain.exception.InvalidUserException;

public class AppMain {
	
	public static void main(String[] args) throws ZKNamingException {

		if(args.length != 6){
			System.err.println("Argumentos inválidos!");
			return;
		}
		
		// receive and print arguments
		System.out.printf("Recebidos %d argumentos%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		AppUser user = new AppUser(args[2], args[3], Float.valueOf(args[4]), Float.valueOf(args[5]));
		try {
			ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
			HubFrontend frontend = new HubFrontend(zkNaming, user, "/grpc/bicloin/hub/1");
			readCommands(System.in, frontend);
		} catch(InvalidUserException e) {
			System.out.println(e.getMessage());
			return;
		}
	}

	private static void readCommands(InputStream inputStream, HubFrontend frontend) {
		Scanner scanner = new Scanner(inputStream);
		System.out.print("> ");
		while(scanner.hasNext()) {
			String command = scanner.nextLine();
			if(command.startsWith("#") || command.equals("")) {
				System.out.print("> ");
				continue;
			}
			
			else if(command.startsWith("zzz")) {
				String[] tokens = command.split(" ");
				Integer timeout = Integer.parseInt(tokens[1]);
				try {
					TimeUnit.MILLISECONDS.sleep(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			else if(command.equals("ping")) {
				System.out.println(frontend.ping());
			}
			
			else if(command.equals("sys-status")) {
				System.out.println(frontend.sysStatus());
			}

			else if(command.equals("balance")) {
				System.out.println(frontend.balance());
			}

			else if(command.startsWith("top-up")) {
				String[] tokens = command.split(" ");
				if(tokens.length != 2) {
					commandNotFound();
					continue;
				}
				Integer value;
				if(!isNumeric(tokens[1])) {
					argumentError();
					continue;
				}
				else {
					value = Integer.parseInt(tokens[1]);
				}

				if(value >= 1 && value <= 20) {
					System.out.println(frontend.topUp(value));
				}
				else {
					System.out.println("ERRO Carregamento deve ser entre 1 e 20 Euros.");
				}
			}
			else if(command.startsWith("info")) {
				String[] tokens = command.split(" ");
				if(tokens.length != 2) {
					commandNotFound();
					continue;
				}
				String abrev = tokens[1];
				System.out.println(frontend.infoStation(abrev));
			}
			else if(command.startsWith("tag")) {
				String[] tokens = command.split(" ");
				if(tokens.length != 4) {
					commandNotFound();
					continue;
				}
				float latitude;
				if(!isFloat(tokens[1])) {
					argumentError();
					continue;
				}
				else {
					latitude = Float.parseFloat(tokens[1]);
				}
				float longitude;
				if(!isFloat(tokens[2])) {
					argumentError();
					continue;
				}
				else {
					longitude = Float.parseFloat(tokens[2]);
				}
				
				String tagName = tokens[3];
				System.out.println(frontend.tag(latitude, longitude, tagName));
			}
			else if(command.startsWith("move")) {
				String[] tokens = command.split(" ");
				if(tokens.length == 2) {
					String tagName = tokens[1];
					System.out.println(frontend.move(tagName));
				}
				else if(tokens.length == 3){
					float latitude;
					if(!isFloat(tokens[1])) {
						argumentError();
						continue;
					}
					else {
						latitude = Float.parseFloat(tokens[1]);
					}
					float longitude;
					if(!isFloat(tokens[2])) {
						argumentError();
						continue;
					}
					else {
						longitude = Float.parseFloat(tokens[2]);
					}
					System.out.println(frontend.move(latitude, longitude));
				}
				else {
					commandNotFound();
					continue;
				}
			}
			else if(command.equals("at")) {
				System.out.println(frontend.at());
			}
			else if(command.startsWith("scan")) {
				String[] tokens = command.split(" ");
				if(tokens.length != 2) {
					commandNotFound();
					continue;
				}
				Integer nStations;
				if(!isNumeric(tokens[1])) {
					argumentError();
					continue;
				}
				else {
					nStations = Integer.parseInt(tokens[1]);
				}
				System.out.println(frontend.scan(nStations));
			}
			else if(command.startsWith("bike-up")) {
				String[] tokens = command.split(" ");
				if(tokens.length != 2) {
					commandNotFound();
					continue;
				}
				String abrev = tokens[1];
				System.out.println(frontend.bikeUp(abrev));
			}
			else if(command.startsWith("bike-down")) {
				String[] tokens = command.split(" ");
				if(tokens.length != 2) {
					commandNotFound();
					continue;
				}
				String abrev = tokens[1];
				System.out.println(frontend.bikeDown(abrev));
			}
			else if(command.equals("help")) {
				String path = "../app/src/main/java/pt/tecnico/bicloin/app/help.txt";
				System.out.println(frontend.printHelp(path));
			}
			else if(command.equals("exit") || command.equals("quit") ){
				System.out.println("OK a fechar aplicação.");
				System.exit(0);
			}
			else if(command.equals("\0")){
				System.exit(0);
			}
			else {
				commandNotFound();
				continue;
			}
			System.out.print("> ");
		}
		scanner.close();
	}

	private static boolean isNumeric(String s) {
        return s != null && s.matches("^[0-9+]*$");
    }

	private static boolean isFloat(String s) {
		return s != null && s.matches("^[-+]?[0-9]*.?[0-9]+$");
    }

	private static void commandNotFound() {
		System.out.println("ERRO Comando não encontrado.");
		System.out.print("> ");
	}

	private static void argumentError() {
		System.out.println("ERRO argumento.");
		System.out.print("> ");
	}

}
