package pt.tecnico.rec;

import java.util.List;

import com.google.protobuf.Message;

import io.grpc.stub.StreamObserver;

public class RecObserver<R> implements StreamObserver<R> {

    private List<Message> responseCollector;
    private Integer instance;

    public RecObserver(List<Message> responseCollector, Integer instance) {
        this.responseCollector = responseCollector;
        this.instance = instance;
    }

    @Override
    public void onNext(R response) {
        synchronized(responseCollector) {
            System.out.printf("Réplica %d do rec enviou %s.%n", this.instance, response.toString());
            responseCollector.add( (Message) response);
            responseCollector.notifyAll();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {
        System.out.println("Request completed");
    }
}