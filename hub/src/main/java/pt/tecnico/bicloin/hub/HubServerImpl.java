package pt.tecnico.bicloin.hub;

import pt.tecnico.bicloin.hub.domain.ImmutableRecords;
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
import pt.tecnico.bicloin.hub.grpc.Hub.*;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.Rec.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.INVALID_ARGUMENT;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

public class HubServerImpl extends HubServiceGrpc.HubServiceImplBase {
    
    private Integer instance;
    private ZKNaming zkNaming;
    private ImmutableRecords immutableRecords;

    private ManagedChannel channel = null;
    private HubServiceGrpc.HubServiceBlockingStub hubStub = null;
    private RecServiceGrpc.RecServiceBlockingStub recStub = null;

    public HubServerImpl(Integer instance, ZKNaming zkNaming, String users, String stations, boolean initRec) throws FileNotFoundException {
        super();
        this.instance = instance;
        this.zkNaming = zkNaming;
        try {
            ZKRecord recRecord = zkNaming.lookup("/grpc/bicloin/rec/1");
            setRec(recRecord);
            immutableRecords = new ImmutableRecords(users, stations, initRec, recStub);
        } catch(ZKNamingException e) {
            System.out.println(e.getMessage());
        }    
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
        if(!immutableRecords.existsUser(username)) {
            responseObserver.onError(INTERNAL.withDescription("Inexistent user.").asRuntimeException());
            return;
        }

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
        String phoneNumber = topUpRequest.getPhone();
        if(!immutableRecords.existsUser(username, phoneNumber)) {
            responseObserver.onError(INTERNAL.withDescription("Inexistent user.").asRuntimeException());
            return;
        }

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

    @Override
    public void infoStation(InfoStationRequest request, StreamObserver<InfoStationResponse> responseObserver) {
        String stationId = request.getStationId();
        if(!immutableRecords.existsStation(stationId)) {
            responseObserver.onError(INTERNAL.withDescription("Inexistent station.").asRuntimeException());
            return;
        }
        Station station = immutableRecords.getStation(stationId);
        String bikesRegisterName = "bikes-" + stationId;
        String bikeUpStatsRegisterName = "bikeUpStats-" + stationId;
        String bikeDownStatsRegisterName = "bikeDownStats-" + stationId;
        ReadRequest bikesReadRequest = ReadRequest.newBuilder().setRegisterName(bikesRegisterName).build();
        ReadRequest bikeUpStatsReadRequest = ReadRequest.newBuilder().setRegisterName(bikeUpStatsRegisterName).build();
        ReadRequest bikeDownStatsReadRequest = ReadRequest.newBuilder().setRegisterName(bikeDownStatsRegisterName).build();
        try {
            ZKRecord recRecord = zkNaming.lookup("/grpc/bicloin/rec/1");
            setRec(recRecord);
            try {
                ReadResponse bikesReadResponse = recStub.read(bikesReadRequest);
                ReadResponse bikeUpStatsReadResponse = recStub.read(bikeUpStatsReadRequest);
                ReadResponse bikeDownStatsReadResponse = recStub.read(bikeDownStatsReadRequest);

                String name = station.getName();
                float latitude = station.getLatitude();
                float longitude = station.getLongitude();
                Integer docks = station.getDocks();
                Integer reward = station.getReward();
                Integer bikes = bikesReadResponse.getRegisterValue().unpack(Bikes.class).getBikes();
                Integer bikeUpStats = bikeUpStatsReadResponse.getRegisterValue().unpack(BikeUpStats.class).getBikeUpStats();
                Integer bikeDownStats = bikeDownStatsReadResponse.getRegisterValue().unpack(BikeDownStats.class).getBikeDownStats();

                
                InfoStationResponse infoStationResponse = InfoStationResponse
                                    .newBuilder()
                                    .setName(name)
                                    .setLatitude(latitude)
                                    .setLongitude(longitude)
                                    .setDocks(docks)
                                    .setReward(reward)
                                    .setBikes(bikes)
                                    .setBikeUpStats(bikeUpStats)
                                    .setBikeDownStats(bikeDownStats)
                                    .build();
                
                if(Context.current().isCancelled()) {
                    return;
                }

                responseObserver.onNext(infoStationResponse);
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
