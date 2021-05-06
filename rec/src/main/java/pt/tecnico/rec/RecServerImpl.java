package pt.tecnico.rec;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.stub.StreamObserver;
import pt.tecnico.rec.domain.Records;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.*;

import pt.tecnico.rec.domain.*;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.INVALID_ARGUMENT;;

public class RecServerImpl extends RecServiceGrpc.RecServiceImplBase {

    private Integer instance;
    private Records records = new Records();

    public RecServerImpl(Integer instance) {
        super();
        this.instance = instance;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        String output = "Réplica " + instance + " do rec está ligada.";
        PingResponse response = PingResponse.newBuilder().setOutput(output).build();
        responseObserver.onNext(response);
		responseObserver.onCompleted();
    }

    public ReadResponse buildReadResponse(String registerName) {
        int lastSlashIndex = registerName.lastIndexOf('-');
        String type = registerName.substring(0, lastSlashIndex);

        if(type.equals("balance")) {
            RecBalance recBalance = records.readBalance(registerName);
            Balance balance = Balance.newBuilder().setBalance(recBalance.getBalance()).build();
            Integer tag = recBalance.getTag();
			Any value = Any.pack(balance);
            
            return ReadResponse.newBuilder().setRegisterValue(value).setTag(tag).build();
        }
        else if(type.equals("bikes")) {
            RecBikes recBikes = records.readBikes(registerName);
            Bikes bikes = Bikes.newBuilder().setBikes(recBikes.getBikes()).build();
            Integer tag = recBikes.getTag();
			Any value = Any.pack(bikes);
            
            return ReadResponse.newBuilder().setRegisterValue(value).setTag(tag).build();
        }
        else if(type.equals("bikeUpStats")) {
            RecBikeUpStats recBikeUpStats = records.readBikeUpStats(registerName);
            BikeUpStats bikeUpStats = BikeUpStats.newBuilder().setBikeUpStats(recBikeUpStats.getBikeUpStats()).build();
            Integer tag = recBikeUpStats.getTag();
			Any value = Any.pack(bikeUpStats);
            
            return ReadResponse.newBuilder().setRegisterValue(value).setTag(tag).build();
        }
        else if(type.equals("bikeDownStats")) {
            RecBikeDownStats recBikeDownStats = records.readBikeDownStats(registerName);
            BikeDownStats bikeDownStats = BikeDownStats.newBuilder().setBikeDownStats(recBikeDownStats.getBikeDownStats()).build();
            Integer tag = recBikeDownStats.getTag();
			Any value = Any.pack(bikeDownStats);
            
            return ReadResponse.newBuilder().setRegisterValue(value).setTag(tag).build();
        }
        else if(type.equals("isBikedUp")) {
            RecIsUserBikedUp recIsUserBikedUp = records.readIsUserBikedUp(registerName);
            IsBikedUp isBikedUp = IsBikedUp.newBuilder().setIsBikedUp(recIsUserBikedUp.getIsUserBikedUp()).build();
            Integer tag = recIsUserBikedUp.getTag();
			Any value = Any.pack(isBikedUp);
            
            return ReadResponse.newBuilder().setRegisterValue(value).setTag(tag).build();
        }

        return null;
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        String registerName = request.getRegisterName(); // balance-username / bikes-abrev / ...

        ReadResponse response = buildReadResponse(registerName);
        
        responseObserver.onNext(response);
		responseObserver.onCompleted();
    }

    public WriteResponse setValue(String registerName, WriteRequest request, StreamObserver<WriteResponse> responseObserver) {
        int lastSlashIndex = registerName.lastIndexOf('-');
        String type = registerName.substring(0, lastSlashIndex);

        if(type.equals("balance")) {
            try {
                Integer balance = request.getValue().unpack(Balance.class).getBalance();
                Integer tag = request.getTag();
                RecBalance recBalance = new RecBalance(balance, tag);
                records.writeBalance(registerName, recBalance);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("bikes")) {
            try {
                Integer bikes = request.getValue().unpack(Bikes.class).getBikes();
                Integer tag = request.getTag();
                RecBikes recBikes = new RecBikes(bikes, tag);
                records.writeBikes(registerName, recBikes);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("bikeUpStats")) {
            try {
                Integer bikeUpStats = request.getValue().unpack(BikeUpStats.class).getBikeUpStats();
                Integer tag = request.getTag();
                RecBikeUpStats recBikeUpStats = new RecBikeUpStats(bikeUpStats, tag);
                records.writeBikeUpStats(registerName, recBikeUpStats);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("bikeDownStats")) {
            try {
                Integer bikeDownStats = request.getValue().unpack(BikeDownStats.class).getBikeDownStats();
                Integer tag = request.getTag();
                RecBikeDownStats recBikeDownStats = new RecBikeDownStats(bikeDownStats, tag);
                records.writeBikeDownStats(registerName, recBikeDownStats);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("isBikedUp")) {
            try {
                Boolean isUserBikedUp = request.getValue().unpack(IsBikedUp.class).getIsBikedUp();
                Integer tag = request.getTag();
                RecIsUserBikedUp recIsUserBikedUp = new RecIsUserBikedUp(isUserBikedUp, tag);
                records.writeIsUserBikedUp(registerName, recIsUserBikedUp);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tipo de registo não conhecido.").asRuntimeException());
        }

        return null;
    }

    @Override
    public void write(WriteRequest request, StreamObserver<WriteResponse> responseObserver) {
        String registerName = request.getRegisterName(); // balance-username / bikes-abrev / ...

        WriteResponse response = setValue(registerName, request, responseObserver);
        responseObserver.onNext(response);
		responseObserver.onCompleted();
    }

    @Override
    public void clean(CleanRequest request, StreamObserver<CleanResponse> responseObserver) {
        records.cleanRecords();
        CleanResponse response = CleanResponse.newBuilder().build();
        responseObserver.onNext(response);
		responseObserver.onCompleted();
    }

}