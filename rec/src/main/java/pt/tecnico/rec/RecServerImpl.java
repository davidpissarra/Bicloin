package pt.tecnico.rec;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.stub.StreamObserver;
import pt.tecnico.rec.domain.Records;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.*;

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
        String output = "Rec instance number " + instance + " is UP.";
        PingResponse response = PingResponse.newBuilder().setOutput(output).build();
        responseObserver.onNext(response);
		responseObserver.onCompleted();
    }

    public ReadResponse buildReadResponse(String registerName) {
        int lastSlashIndex = registerName.lastIndexOf('-');
        String type = registerName.substring(0, lastSlashIndex);

        if(type.equals("balance")) {
            Balance balance = Balance.newBuilder().setBalance(records.readBalance(registerName)).build();
			Any value = Any.pack(balance);
            return ReadResponse.newBuilder().setRegisterValue(value).build();
        }
        else if(type.equals("bikes")) {
            Bikes bikes = Bikes.newBuilder().setBikes(records.readBikes(registerName)).build();
			Any value = Any.pack(bikes);
            return ReadResponse.newBuilder().setRegisterValue(value).build();
        }
        else if(type.equals("bikeUpStats")) {
            BikeUpStats bikeUpStats = BikeUpStats.newBuilder().setBikeUpStats(records.readBikeUpStats(registerName)).build();
			Any value = Any.pack(bikeUpStats);
            return ReadResponse.newBuilder().setRegisterValue(value).build();
        }
        else if(type.equals("bikeDownStats")) {
            BikeDownStats bikeDownStats = BikeDownStats.newBuilder().setBikeDownStats(records.readBikeDownStats(registerName)).build();
			Any value = Any.pack(bikeDownStats);
            return ReadResponse.newBuilder().setRegisterValue(value).build();
        }
        else if(type.equals("isBikedUp")) {
            IsBikedUp isBikedUp = IsBikedUp.newBuilder().setIsBikedUp(records.readIsUserBikedUp(registerName)).build();
			Any value = Any.pack(isBikedUp);
            return ReadResponse.newBuilder().setRegisterValue(value).build();
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
                records.writeBalance(registerName, balance);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("bikes")) {
            try {
                Integer bikes = request.getValue().unpack(Bikes.class).getBikes();
                records.writeBikes(registerName, bikes);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("bikeUpStats")) {
            try {
                Integer bikeUpStats = request.getValue().unpack(BikeUpStats.class).getBikeUpStats();
                records.writeBikeUpStats(registerName, bikeUpStats);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("bikeDownStats")) {
            try {
                Integer bikeDownStats = request.getValue().unpack(BikeDownStats.class).getBikeDownStats();
                records.writeBikeDownStats(registerName, bikeDownStats);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
        else if(type.equals("isBikedUp")) {
            try {
                Boolean isBikedUp = request.getValue().unpack(IsBikedUp.class).getIsBikedUp();
                records.writeIsUserBikedUp(registerName, isBikedUp);
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