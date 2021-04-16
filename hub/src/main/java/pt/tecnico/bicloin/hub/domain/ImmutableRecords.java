package pt.tecnico.bicloin.hub.domain;

import java.util.List;
import java.util.Scanner;

import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.domain.exception.ErrorMessage;
import pt.tecnico.bicloin.hub.domain.exception.InvalidStationException;
import pt.tecnico.rec.RecFrontend;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ImmutableRecords {
    private static final ErrorMessage INVALID_NUMBER_BIKES_AVAILABLE = null;
    private final List<User> users = new ArrayList<>();
    private final List<Station> stations = new ArrayList<>();

    public ImmutableRecords(String users, String stations, boolean initRec, RecFrontend frontend) throws FileNotFoundException {
        importUsers(users, initRec, frontend);
        importStations(stations, initRec, frontend);
    }

    public List<Station> getStations() {
        return stations;
    }

    private void importUsers(String usersFilename, boolean initRec, RecFrontend frontend) throws FileNotFoundException {
        String path = "src/main/java/pt/tecnico/bicloin/hub/" + usersFilename;
        Scanner scanner = new Scanner(new File(path));
        while(scanner.hasNext()) {
            String[] args = scanner.nextLine().split(",");
            String id = args[0];
            String name = args[1];
            String phoneNumber = args[2];
            users.add(new User(id, name, phoneNumber));
            if(initRec) {
                createUserRecords(id, frontend); 
            }
        }
        scanner.close();
    }

    private void importStations(String stationsFilename, boolean initRec, RecFrontend frontend) throws FileNotFoundException {
        String path = "src/main/java/pt/tecnico/bicloin/hub/" + stationsFilename;
        Scanner scanner = new Scanner(new File(path));
        while(scanner.hasNext()) {
            String[] args = scanner.nextLine().split(",");
            String name = args[0];
            String abrev = args[1];
            float latitude = Float.parseFloat(args[2]);
            float longitude = Float.parseFloat(args[3]);
            Integer docks = Integer.parseInt(args[4]);
            Integer bikesAvailable = Integer.parseInt(args[5]);
            Integer reward = Integer.parseInt(args[6]);

            if(bikesAvailable > docks) {
                throw new InvalidStationException(INVALID_NUMBER_BIKES_AVAILABLE, abrev);
            }

            stations.add(new Station(name, abrev, latitude, longitude, docks, reward));
            if(initRec) {
                createStationRecords(bikesAvailable, abrev, frontend); 
            }
        }
        scanner.close();
    }

    public void createUserRecords(String abrev, RecFrontend frontend) {
        String isBikedUpRegisterName = "isBikedUp-" + abrev;
    
        try {
            frontend.writeIsBikedUp(isBikedUpRegisterName, false);
        } catch(StatusRuntimeException e) {
            System.out.println("Rec instance is DOWN.\n");
        } catch (InvalidProtocolBufferException e) {
            System.out.println(e.getMessage());
        }
    }

    public void createStationRecords(Integer bikesAvailable, String abrev, RecFrontend frontend) {
        String bikesRegisterName = "bikes-" + abrev;
        String bikeUpStatsRegisterName = "bikeUpStats-" + abrev;
        String bikeDownStatsRegisterName = "bikeDownStats-" + abrev;
        try {
            frontend.writeBikes(bikesRegisterName, bikesAvailable);
            frontend.writeBikeUpStats(bikeUpStatsRegisterName, 0);
            frontend.writeBikeDownStats(bikeDownStatsRegisterName, 0);
        } catch(StatusRuntimeException e) {
            System.out.println("Rec instance is DOWN.\n");
        } catch (InvalidProtocolBufferException e) {
            System.out.println(e.getMessage());
        }
    }

    public Station getStation(String stationId) {
        return stations.stream().filter(station -> station.getAbrev().equals(stationId)).findFirst().get();
    }

    public boolean existsUser(String id) {
        return users.stream().anyMatch(user -> user.equals(id));
    }

    public boolean existsUser(String id, String phoneNumber) {
        return users.stream().anyMatch(user -> user.equals(id, phoneNumber));
    }

    public boolean existsStation(String id) {
        return stations.stream().anyMatch(station -> station.equals(id));
    }
}