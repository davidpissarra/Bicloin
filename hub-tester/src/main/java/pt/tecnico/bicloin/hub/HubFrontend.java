package pt.tecnico.bicloin.hub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.tecnico.bicloin.hub.grpc.Hub.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class HubFrontend implements AutoCloseable {

    private final ManagedChannel channel;
    private final HubServiceGrpc.HubServiceBlockingStub stub;

    public HubFrontend(String zooHost, String zooPort, String path) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
        ZKRecord record = zkNaming.lookup(path);
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = HubServiceGrpc.newBlockingStub(channel);
    }

    public PingResponse ping(PingRequest request) {
        return stub.ping(request);
    }

    public SysStatusResponse sysStatus(SysStatusRequest request) {
        return stub.sysStatus(request);
    }

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}
