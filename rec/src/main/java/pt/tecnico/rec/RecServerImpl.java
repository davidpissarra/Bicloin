package pt.tecnico.rec;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.stub.StreamObserver;
import pt.tecnico.rec.domain.Records;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.*;

import static io.grpc.Status.INTERNAL;

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
            Balance balance = Balance.newBuilder().setBalance( (Integer) records.readBalance(registerName) ).build();
			Any value = Any.pack(balance);
            return ReadResponse.newBuilder().setRegisterValue(value).build();
        }
        return null;
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        String registerName = request.getRegisterName(); // balance-username / bikes-abrev

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
                Integer stationBikes = request.getValue().unpack(StationBikes.class).getStationBikes();
                System.out.println(stationBikes);
                records.writeStationBikes(registerName, stationBikes);
                return WriteResponse.newBuilder().setRegisterValue(request.getValue()).build();
            } catch (InvalidProtocolBufferException e) {
                responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }

        return null;
    }

    @Override
    public void write(WriteRequest request, StreamObserver<WriteResponse> responseObserver) {
        String registerName = request.getRegisterName(); // balance-username / bikes-abrev

        WriteResponse response = setValue(registerName, request, responseObserver);
        responseObserver.onNext(response);
		responseObserver.onCompleted();
    }

}