package pt.tecnico.bicloin.hub.domain;

import java.util.List;
import java.util.Scanner;

import com.google.protobuf.Any;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.domain.exception.ErrorMessage;
import pt.tecnico.bicloin.hub.domain.exception.InvalidStationException;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.BikeDownStats;
import pt.tecnico.rec.grpc.Rec.BikeUpStats;
import pt.tecnico.rec.grpc.Rec.Bikes;
import pt.tecnico.rec.grpc.Rec.WriteRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import static pt.tecnico.bicloin.hub.domain.exception.ErrorMessage.*;

public class ImmutableRecords {
    private static final ErrorMessage INVALID_NUMBER_BIKES_AVAILABLE = null;
    private final List<User> users = new ArrayList<>();
    private final List<Station> stations = new ArrayList<>();

    public ImmutableRecords(String users, String stations, boolean initRec, RecServiceGrpc.RecServiceBlockingStub stub) throws FileNotFoundException {
        importUsers(users);
        importStations(stations, initRec, stub);
    }

    private void importUsers(String usersFilename) throws FileNotFoundException {
        String path = "src/main/java/pt/tecnico/bicloin/hub/" + usersFilename;
        Scanner scanner = new Scanner(new File(path));
        while(scanner.hasNext()) {
            String[] args = scanner.nextLine().split(",");
            String id = args[0];
            String name = args[1];
            String phoneNumber = args[2];
            users.add(new User(id, name, phoneNumber));
        }
        scanner.close();
    }

    private void importStations(String stationsFilename, boolean initRec, RecServiceGrpc.RecServiceBlockingStub stub) throws FileNotFoundException {
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
                createStationRecords(bikesAvailable, abrev, stub); 
            }
        }
        scanner.close();
    }

    public void createStationRecords(Integer bikesAvailable, String abrev, RecServiceGrpc.RecServiceBlockingStub stub) {
        String bikesRegisterName = "bikes-" + abrev;
        String bikeUpStatsRegisterName = "bikeUpStats-" + abrev;
        String bikeDownStatsRegisterName = "bikeDownStats-" + abrev;
        Bikes bikes = Bikes.newBuilder().setBikes(bikesAvailable).build();
        BikeUpStats bikeUpStats = BikeUpStats.newBuilder().setBikeUpStats(0).build();
        BikeDownStats bikeDownStats = BikeDownStats.newBuilder().setBikeDownStats(0).build();
        WriteRequest bikesWriteRequest = WriteRequest
                                        .newBuilder()
                                        .setRegisterName(bikesRegisterName)
                                        .setValue(Any.pack(bikes))
                                        .build();
        WriteRequest bikeUpStatsWriteRequest = WriteRequest
                                        .newBuilder()
                                        .setRegisterName(bikeUpStatsRegisterName)
                                        .setValue(Any.pack(bikeUpStats))
                                        .build();
        WriteRequest bikeDownStatsWriteRequest = WriteRequest
                                        .newBuilder()
                                        .setRegisterName(bikeDownStatsRegisterName)
                                        .setValue(Any.pack(bikeDownStats))
                                        .build();
        try {
            stub.write(bikesWriteRequest);
            stub.write(bikeUpStatsWriteRequest);
            stub.write(bikeDownStatsWriteRequest);
        } catch(StatusRuntimeException e) {
            System.out.println("Rec instance is DOWN.\n");
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