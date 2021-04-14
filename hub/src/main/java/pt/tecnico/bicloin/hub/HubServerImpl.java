package pt.tecnico.bicloin.hub;

import pt.tecnico.bicloin.hub.domain.User;
import pt.tecnico.bicloin.hub.domain.exception.InvalidStationException;
import pt.tecnico.bicloin.hub.domain.Station;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.tecnico.bicloin.hub.grpc.Hub.BalanceRequest;
import pt.tecnico.bicloin.hub.grpc.Hub.BalanceResponse;
import pt.tecnico.bicloin.hub.grpc.Hub.TopUpRequest;
import pt.tecnico.bicloin.hub.grpc.Hub.TopUpResponse;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.Balance;
import pt.tecnico.rec.grpc.Rec.ReadRequest;
import pt.tecnico.rec.grpc.Rec.ReadResponse;
import pt.tecnico.bicloin.hub.grpc.Hub;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.Rec.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import static io.grpc.Status.INTERNAL;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import static pt.tecnico.bicloin.hub.domain.exception.ErrorMessage.*;

public class HubServerImpl extends HubServiceGrpc.HubServiceImplBase {
    
    private Integer instance;
    private ZKNaming zkNaming;
    private final List<User> users;
    private final List<Station> stations;

    private ManagedChannel channel = null;
    private HubServiceGrpc.HubServiceBlockingStub hubStub = null;
    private RecServiceGrpc.RecServiceBlockingStub recStub = null;

    public HubServerImpl(Integer instance, ZKNaming zkNaming, String users, String stations, boolean initRec) throws FileNotFoundException {
        super();
        this.instance = instance;
        this.zkNaming = zkNaming;
        this.users = importUsers(users);
        this.stations = importStations(stations, initRec);
    }

    private List<User> importUsers(String usersFilename) throws FileNotFoundException {
        List<User> users = new ArrayList<>();
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
        return users;
    }

    private List<Station> importStations(String stationsFilename, boolean initRec) throws FileNotFoundException {
        List<Station> stations = new ArrayList<>();
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
                String registerName = "bikes-" + abrev;
                StationBikes stationBikes = StationBikes.newBuilder().setStationBikes(bikesAvailable).build();
                WriteRequest writeRequest = WriteRequest
                                                .newBuilder()
                                                .setRegisterName(registerName)
                                                .setValue(Any.pack(stationBikes))
                                                .build();
                try {
                    ZKRecord recRecord = zkNaming.lookup("/grpc/bicloin/rec/1");
                    setRec(recRecord);
                    try {
                        recStub.write(writeRequest);
                    } catch(StatusRuntimeException e) {
                        System.out.println("Rec instance number " + getInstanceNumber(recRecord) + " is DOWN.\n");
                    }
                } catch(ZKNamingException e) {
                    System.out.println(e.getMessage());
                }    
            }
        }
        scanner.close();
        return stations;
    }

    private void setHub(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.hubStub = HubServiceGrpc.newBlockingStub(channel);
    }

    private void setRec(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.recStub = RecServiceGrpc.newBlockingStub(channel);
    }

    private Integer getInstanceNumber(ZKRecord record) {
        String path = record.getPath();
        Integer lastSlashIndex = path.lastIndexOf('/');
		return Integer.valueOf(path.substring(lastSlashIndex + 1));
    }

    @Override
    public void ping(Hub.PingRequest request, StreamObserver<Hub.PingResponse> responseObserver) {
        String output = "Hub instance number " + instance + " is UP.";
        Hub.PingResponse response = Hub.PingResponse.newBuilder().setOutput(output).build();

        if(Context.current().isCancelled()) {
            return;
        }
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sysStatus(Hub.SysStatusRequest request, StreamObserver<Hub.SysStatusResponse> responseObserver) {
        try {
            Collection<ZKRecord> recRecords = zkNaming.listRecords("/grpc/bicloin/rec");
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");

            String output = getOutput(recRecords, hubRecords);

            Hub.SysStatusResponse response = Hub.SysStatusResponse.newBuilder().setOutput(output).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        
        } catch(ZKNamingException e) {
            responseObserver.onError(INTERNAL.withDescription("Internal error.").asRuntimeException());
        }
    }

    private String getOutput(Collection<ZKRecord> recRecords, Collection<ZKRecord> hubRecords) {
        String output = "";
        output = pingHubs(hubRecords, output);
        output = pingRecs(recRecords, output);
        return output;
    }

    private String pingRecs(Collection<ZKRecord> recRecords, String output) {
        Rec.PingRequest recRequest = Rec.PingRequest.newBuilder().build();
        for(ZKRecord rec : recRecords) {
            setRec(rec);
            try {
                Rec.PingResponse recResponse = recStub.ping(recRequest);
                output += recResponse.getOutput() + "\n";
            } catch(StatusRuntimeException e) {
                output += "Rec instance number " + getInstanceNumber(rec) + " is DOWN.\n";
            }
        }
        return output;
    }

    private String pingHubs(Collection<ZKRecord> hubRecords, String output) {
        Hub.PingRequest hubRequest = Hub.PingRequest.newBuilder().build();
        for(ZKRecord hub : hubRecords) {
            setHub(hub);
            try {
                Hub.PingResponse hubResponse = hubStub.ping(hubRequest);
                output += hubResponse.getOutput() + "\n";
            } catch(StatusRuntimeException e) {
                output += "Hub instance number " + getInstanceNumber(hub) + " is DOWN.\n";
            } 
        }
        return output;
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String username = request.getUsername();
        // TODO: check if username in users

        String registerName = "balance-" + username;
        ReadRequest readRequest = ReadRequest.newBuilder().setRegisterName(registerName).build();
        try {
            ZKRecord recRecord = zkNaming.lookup("/grpc/bicloin/rec/1");
            setRec(recRecord);
            try {
                ReadResponse readResponse = recStub.read(readRequest);
                Integer balance = readResponse.getRegisterValue().unpack(Balance.class).getBalance();
                BalanceResponse balanceResponse = BalanceResponse.newBuilder().setBalance(balance).build();

                if(Context.current().isCancelled()) {
                    return;
                }

                responseObserver.onNext(balanceResponse);
                responseObserver.onCompleted();
                return;
            } catch(StatusRuntimeException | InvalidProtocolBufferException e) {
                System.out.println("Rec instance number " + getInstanceNumber(recRecord) + " is DOWN.\n");
            }
        } catch(ZKNamingException e) {
            responseObserver.onError(INTERNAL.withDescription("Internal error.").asRuntimeException());
        }    
    }

    @Override
    public void topUp(TopUpRequest topUpRequest, StreamObserver<TopUpResponse> responseObserver) {
        String username = topUpRequest.getUsername();
        String phoneNumbeString = topUpRequest.getPhone();
        // TODO: check if username and phoneNumber in users

        String registerName = "balance-" + topUpRequest.getUsername();
        ReadRequest readRequest = ReadRequest.newBuilder().setRegisterName(registerName).build();
        try {
            ZKRecord recRecord = zkNaming.lookup("/grpc/bicloin/rec/1");
            setRec(recRecord);
            try {
                ReadResponse readResponse = recStub.read(readRequest);
                Balance balanceMessage = readResponse.getRegisterValue().unpack(Balance.class);
                balanceMessage = Balance.newBuilder()
                                    .setBalance(balanceMessage.getBalance() + topUpRequest.getAmount() * 10)
                                    .build();
                WriteRequest writeRequest = WriteRequest
                                                .newBuilder()
                                                .setRegisterName(registerName)
                                                .setValue(Any.pack(balanceMessage))
                                                .build();
                WriteResponse writeResponse = recStub.write(writeRequest);
                Integer balance = writeResponse.getRegisterValue().unpack(Balance.class).getBalance();
                TopUpResponse topUpResponse = TopUpResponse.newBuilder().setBalance(balance).build();

                if(Context.current().isCancelled()) {
                    return;
                }

                responseObserver.onNext(topUpResponse);
                responseObserver.onCompleted();
                return;
            } catch(StatusRuntimeException | InvalidProtocolBufferException e) {
                System.out.println("Rec instance number " + getInstanceNumber(recRecord) + " is DOWN.\n");
            }
        } catch(ZKNamingException e) {
            responseObserver.onError(INTERNAL.withDescription("Internal error.").asRuntimeException());
        }    
    }
}
