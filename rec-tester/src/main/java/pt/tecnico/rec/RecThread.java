package pt.tecnico.rec;

import com.google.protobuf.Message;


import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.ReadRequest;
import pt.tecnico.rec.grpc.Rec.ReadResponse;
import pt.tecnico.rec.grpc.Rec.WriteRequest;
import pt.tecnico.rec.grpc.Rec.WriteResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;


public class RecThread extends Thread {

    private List<Message> responseCollector;
    private Message requestMessage;
    private ManagedChannel channel;
    private RecServiceGrpc.RecServiceStub stub;
    private Integer instance;
    
    
    public RecThread(List<Message> responseCollector, ZKRecord record, Message requestMessage) {
        this.responseCollector = responseCollector;
        this.requestMessage = requestMessage;
        setRec(record);
        setInstance(record.getPath());
    }

    public void setRec(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = RecServiceGrpc.newStub(channel);
    }

    public void setInstance(String path) {
        Integer lastIndex = path.lastIndexOf("/");
        this.instance = Integer.valueOf(path.substring(lastIndex + 1));
    }

    public void run() throws StatusRuntimeException {
        System.out.printf("A contactar réplica %d do rec.%n", this.instance);
        if(requestMessage instanceof ReadRequest && this.channel != null) {
            ReadRequest request = (ReadRequest) requestMessage;
            stub.read(request, new RecObserver<ReadResponse>(responseCollector, instance));
        }
        else if(requestMessage instanceof WriteRequest && this.channel != null) {
            WriteRequest request = (WriteRequest) requestMessage;
            stub.write(request, new RecObserver<WriteResponse>(responseCollector, instance));
        }
    }

    public void shutDownChannel() {
        channel.shutdown();
    }

}
