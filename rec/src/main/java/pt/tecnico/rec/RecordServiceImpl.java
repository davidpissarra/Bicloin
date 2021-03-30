package pt.tecnico.rec;

import io.grpc.stub.StreamObserver;

public class RecordServiceImpl extends RecordServiceGrpc.RecordServiceImplBase {
    private Records records = new Records();

    @Override
    public read(ReadRequest readRequest, StreamObserver<ReadResponse> responseObserver) {

        UserRecord user = records.getUserRecord(readRequest.getUsername());

        ReadResponse response = ReadResponse.newBuilder()
                                                .setUsername(user.getUsername())
                                                .setName(user.getName())
                                                .setPhoneNumber(user.getPhoneNumber())
                                                .setBalance(user.getBalance())
                                                .build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
    }
}