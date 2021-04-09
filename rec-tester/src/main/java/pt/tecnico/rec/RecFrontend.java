package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class RecFrontend implements AutoCloseable {

    private final ManagedChannel channel;
    private final RecServiceGrpc.RecServiceBlockingStub stub;

    public RecFrontend(String zooHost, String zooPort, String path) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
        ZKRecord record = zkNaming.lookup(path);
        String target = record.getURI();

        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = RecServiceGrpc.newBlockingStub(channel);
    }

    public PingResponse ping(PingRequest request) {
        return stub.ping(request);
    }

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}
