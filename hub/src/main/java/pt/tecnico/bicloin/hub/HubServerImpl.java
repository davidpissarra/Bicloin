package pt.tecnico.bicloin.hub;

import pt.tecnico.bicloin.hub.domain.ImmutableRecords;
import pt.tecnico.bicloin.hub.domain.Station;
import pt.tecnico.bicloin.hub.domain.StationDistance;
import pt.tecnico.bicloin.hub.domain.Haversine;
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
import pt.tecnico.rec.RecFrontend;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.Rec.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.FAILED_PRECONDITION;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

public class HubServerImpl extends HubServiceGrpc.HubServiceImplBase {
    
    private RecFrontend recFrontend;
    private ImmutableRecords immutableRecords;
    private Integer instance;

    public HubServerImpl(Integer instance, RecFrontend recFrontend,
                            String users, String stations, boolean initRec)
                            throws FileNotFoundException {
        super();
        this.instance = instance;
        this.recFrontend = recFrontend;
        immutableRecords = new ImmutableRecords(users, stations, initRec, recFrontend);
    }

    private Integer getHubInstance() {
        return instance;
    }

    @Override
    public void ping(Hub.PingRequest request, StreamObserver<Hub.PingResponse> responseObserver) {
        String output = "Servidor hub est?? ligado.";
        Hub.PingResponse response = Hub.PingResponse.newBuilder().setOutput(output).build();

        if(Context.current().isCancelled()) {
            return;
        }
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sysStatus(Hub.SysStatusRequest request, StreamObserver<Hub.SysStatusResponse> responseObserver) {
        Collection<ZKRecord> recRecords = recFrontend.getRecords();
        
        String output = "Servidor hub est?? ligado.\n";

        output = pingRecs(recRecords, output);

        Hub.SysStatusResponse response = Hub.SysStatusResponse.newBuilder().setOutput(output).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private String pingRecs(Collection<ZKRecord> recRecords, String output) {
        for(ZKRecord rec : recRecords) {
            try {
                output += recFrontend.ping(rec) + "\n";
            } catch(StatusRuntimeException e) {
                output += "ERRO Rec incontact??vel.\n";
            }
        }
        return output;
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String username = request.getUsername();
        if(!immutableRecords.existsUser(username)) {
            responseObserver.onError(INTERNAL.withDescription("Utilizador inexistente.").asRuntimeException());
            return;
        }

        String registerName = "balance-" + username;
        try {
            Integer balance = recFrontend.readBalance(registerName);
            BalanceResponse balanceResponse = BalanceResponse.newBuilder().setBalance(balance).build();

            if(Context.current().isCancelled()) {
                return;
            }

            responseObserver.onNext(balanceResponse);
            responseObserver.onCompleted();
            return;
        } catch(StatusRuntimeException | InvalidProtocolBufferException e) {
            responseObserver.onError(INTERNAL.withDescription("ERRO interno.").asRuntimeException());
        }
    }

    @Override
    public void topUp(TopUpRequest topUpRequest, StreamObserver<TopUpResponse> responseObserver) {
        String username = topUpRequest.getUsername();
        String phoneNumber = topUpRequest.getPhone();
        
        if(!immutableRecords.existsUser(username, phoneNumber)) {
            responseObserver.onError(INTERNAL.withDescription("ERRO Utilizador inexistente.").asRuntimeException());
            return;
        }

        String registerName = "balance-" + username;
        try {
            Integer currentBalance = recFrontend.readBalance(registerName);
            Integer topUpBalance = recFrontend.writeBalance(registerName, currentBalance + topUpRequest.getAmount() * 10, false);
            TopUpResponse topUpResponse = TopUpResponse.newBuilder().setBalance(topUpBalance).build();

            if(Context.current().isCancelled()) {
                return;
            }

            responseObserver.onNext(topUpResponse);
            responseObserver.onCompleted();
            return;
        } catch(StatusRuntimeException | InvalidProtocolBufferException e) {
            responseObserver.onError(INTERNAL.withDescription("ERRO Rec incontact??vel.").asRuntimeException());
        }
    }

    @Override
    public void infoStation(InfoStationRequest request, StreamObserver<InfoStationResponse> responseObserver) {
        String stationId = request.getStationId();
        if(!immutableRecords.existsStation(stationId)) {
            responseObserver.onError(INTERNAL.withDescription("ERRO Esta????o inexistente.").asRuntimeException());
            return;
        }
        Station station = immutableRecords.getStation(stationId);
        String bikesRegisterName = "bikes-" + stationId;
        String bikeUpStatsRegisterName = "bikeUpStats-" + stationId;
        String bikeDownStatsRegisterName = "bikeDownStats-" + stationId;
        try {
            String name = station.getName();
            float latitude = station.getLatitude();
            float longitude = station.getLongitude();
            Integer docks = station.getDocks();
            Integer reward = station.getReward();
            Integer bikes = recFrontend.readBikes(bikesRegisterName);
            Integer bikeUpStats = recFrontend.readBikeUpStats(bikeUpStatsRegisterName);
            Integer bikeDownStats = recFrontend.readBikeDownStats(bikeDownStatsRegisterName);

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
            responseObserver.onError(INTERNAL.withDescription("ERRO Rec incontact??vel.").asRuntimeException());
        }
    }

    @Override
    public void locateStation(LocateStationRequest request, StreamObserver<LocateStationResponse> responseObserver) {
        float latitude = request.getLatitude();
        float longitude = request.getLongitude();
        Integer nStations = request.getNStations();
        List<Station> stations =  immutableRecords.getStations();

        List<StationDistance> closestStations = Haversine.getClosestStations(latitude, longitude, stations, nStations);

        List<StationProtoMessage> protoStations = new ArrayList<>();

        
        closestStations.forEach(stationDistance -> {
            Station station = immutableRecords.getStation(stationDistance.getAbrev());

            String bikesRegisterName = "bikes-" + station.getAbrev();
            try {
                Integer bikes = recFrontend.readBikes(bikesRegisterName);
                
                if(Context.current().isCancelled()) {
                    return;
                }
                StationProtoMessage message = StationProtoMessage
                                                .newBuilder()
                                                .setAbrev(station.getAbrev())
                                                .setLatitude(station.getLatitude())
                                                .setLongitude(station.getLongitude())
                                                .setDocks(station.getDocks())
                                                .setReward(station.getReward())
                                                .setBikes(bikes)
                                                .setDistance(stationDistance.getDistance())
                                                .build();
                protoStations.add(message);
            } catch(StatusRuntimeException | InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription("ERRO Rec incontact??vel.").asRuntimeException());
            }                        
        });

        LocateStationResponse response = LocateStationResponse
                                            .newBuilder()
                                            .addAllStations(protoStations)
                                            .build();
        
        if(Context.current().isCancelled()) {
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void bikeUp(BikeUpRequest request, StreamObserver<BikeUpResponse> responseObserver) {
        String username = request.getUsername();
        float userLatitude = request.getUserLatitude();
        float userLongitude = request.getUserLongitude();
        String stationAbrev = request.getStationId();
        
        if(!immutableRecords.existsStation(stationAbrev)) {
            responseObserver.onError(INTERNAL.withDescription("ERRO Esta????o inexistente.").asRuntimeException());
            return;
        }
        
        Station station = immutableRecords.getStation(stationAbrev);
        
        if(!Haversine.inRangeStation(userLatitude, userLongitude, station.getLatitude(), station.getLongitude())) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("ERRO fora de alcance.").asRuntimeException());
            return;
        }

        try {            
            String isBikedUpRegisterName = "isBikedUp-" + username;
            Boolean isBikedUp = recFrontend.readIsBikedUp(isBikedUpRegisterName);
            
            String bikesRegisterName = "bikes-" + station.getAbrev();
            Integer bikes = recFrontend.readBikes(bikesRegisterName);

            String balanceRegisterName = "balance-" + username;
            Integer balance = recFrontend.readBalance(balanceRegisterName);

            String bikeUpStatsRegisterName = "bikeUpStats-" + station.getAbrev();
            Integer bikeUpStats = recFrontend.readBikeUpStats(bikeUpStatsRegisterName);

            if(isBikedUp == true) {
                responseObserver.onError(FAILED_PRECONDITION.withDescription("ERRO utilizador j?? tem uma bicicleta levantada.").asRuntimeException());
                return;
            }
            if(bikes == 0) {
                responseObserver.onError(FAILED_PRECONDITION.withDescription("ERRO esta????o sem bicicletas dispon??veis.").asRuntimeException());
                return;
            }
            if(balance < 10) {
                responseObserver.onError(FAILED_PRECONDITION.withDescription("ERRO saldo insuficiente.").asRuntimeException());
                return;
            }

            recFrontend.writeIsBikedUp(isBikedUpRegisterName, true, false);
            recFrontend.writeBikes(bikesRegisterName, bikes - 1, false);
            recFrontend.writeBalance(balanceRegisterName, balance - 10, false);
            recFrontend.writeBikeUpStats(bikeUpStatsRegisterName, bikeUpStats + 1, false);

            if(Context.current().isCancelled()) {
                recFrontend.writeIsBikedUp(isBikedUpRegisterName, false, false);
                recFrontend.writeBikes(bikesRegisterName, bikes, false);
                recFrontend.writeBalance(balanceRegisterName, balance, false);
                recFrontend.writeBikeUpStats(bikeUpStatsRegisterName, bikeUpStats, false);
                return;
            }

            BikeUpResponse response = BikeUpResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(StatusRuntimeException | InvalidProtocolBufferException e) {
            responseObserver.onError(INTERNAL.withDescription("ERRO Rec incontact??vel.").asRuntimeException());
        }
    }

    @Override
    public void bikeDown(BikeDownRequest request, StreamObserver<BikeDownResponse> responseObserver) {
        String username = request.getUsername();
        float userLatitude = request.getUserLatitude();
        float userLongitude = request.getUserLongitude();
        String stationAbrev = request.getStationId();

        if(!immutableRecords.existsStation(stationAbrev)) {
            responseObserver.onError(INTERNAL.withDescription("ERRO Esta????o inexistente.").asRuntimeException());
            return;
        }
        
        Station station = immutableRecords.getStation(stationAbrev);

        if(!Haversine.inRangeStation(userLatitude, userLongitude, station.getLatitude(), station.getLongitude())) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("ERRO fora de alcance.").asRuntimeException());
            return;
        }

        try {
            String isBikedUpRegisterName = "isBikedUp-" + username;
            Boolean isBikedUp = recFrontend.readIsBikedUp(isBikedUpRegisterName);
            
            String bikesRegisterName = "bikes-" + station.getAbrev();
            Integer bikes = recFrontend.readBikes(bikesRegisterName);

            String balanceRegisterName = "balance-" + username;
            Integer balance = recFrontend.readBalance(balanceRegisterName);

            String bikeDownStatsRegisterName = "bikeDownStats-" + station.getAbrev();
            Integer bikeDownStats = recFrontend.readBikeDownStats(bikeDownStatsRegisterName);

            if(isBikedUp == false) {
                responseObserver.onError(FAILED_PRECONDITION.withDescription("ERRO utilizador n??o tem uma bicicleta levantada.").asRuntimeException());
                return;
            }
            if(bikes == station.getDocks()) {
                responseObserver.onError(FAILED_PRECONDITION.withDescription("ERRO esta????o com docas cheias.").asRuntimeException());
                return;
            }

            recFrontend.writeIsBikedUp(isBikedUpRegisterName, false, false);
            recFrontend.writeBikes(bikesRegisterName, bikes + 1, false);
            recFrontend.writeBalance(balanceRegisterName, balance + station.getReward(), false);
            recFrontend.writeBikeDownStats(bikeDownStatsRegisterName, bikeDownStats + 1, false);

            if(Context.current().isCancelled()) {
                recFrontend.writeIsBikedUp(isBikedUpRegisterName, true, false);
                recFrontend.writeBikes(bikesRegisterName, bikes, false);
                recFrontend.writeBalance(balanceRegisterName, balance, false);
                recFrontend.writeBikeDownStats(bikeDownStatsRegisterName, bikeDownStats, false);
                return;
            }

            BikeDownResponse response = BikeDownResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(StatusRuntimeException | InvalidProtocolBufferException e) {
            responseObserver.onError(INTERNAL.withDescription("ERRO Rec incontact??vel.").asRuntimeException());
        }
    }

}
