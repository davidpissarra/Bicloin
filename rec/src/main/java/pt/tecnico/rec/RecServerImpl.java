package pt.tecnico.rec;

import io.grpc.stub.StreamObserver;

import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.PingRequest;
import pt.tecnico.rec.grpc.Rec.PingResponse;

public class RecServerImpl extends RecServiceGrpc.RecServiceImplBase {

    private Integer instance;

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

}