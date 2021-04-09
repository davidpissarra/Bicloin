package pt.tecnico.bicloin.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.grpc.Hub.*;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class App implements AutoCloseable {
    private ManagedChannel channel = null;
    private HubServiceGrpc.HubServiceBlockingStub stub = null;
    private ZKNaming zkNaming;
    private List<ZKRecord> seenHubs = new ArrayList<>();
    private ZKRecord record;

    public App(String zooHost, String zooPort) throws ZKNamingException {
        zkNaming = new ZKNaming(zooHost, zooPort);
    }

    private void setHub(String target) {
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = HubServiceGrpc.newBlockingStub(channel);
    }


    public PingResponse ping() {
        PingRequest pingRequest = PingRequest.newBuilder().build();
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
			return tryPing(hubRecords, pingRequest);
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    private PingResponse tryPing(Collection<ZKRecord> hubRecords, PingRequest pingRequest) {
        try {
            for(ZKRecord record : hubRecords) {
                this.record = record;
                String target = record.getURI();
                setHub(target);
                if(seenHubs.contains(record)) {
                    continue;
                }
                PingResponse pingResponse = this.stub.ping(pingRequest);
                seenHubs.clear();
                return pingResponse;
            }
        } catch (StatusRuntimeException e) {
            System.out.println("RPC FAILED.");
            seenHubs.add(this.record);
            tryPing(hubRecords, pingRequest);
        }
        return null;
    }

    public SysStatusResponse sysStatus() {
        SysStatusRequest sysStatusRequest = SysStatusRequest.newBuilder().build();
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            return trySysStatus(hubRecords, sysStatusRequest);
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    private SysStatusResponse trySysStatus(Collection<ZKRecord> hubRecords, SysStatusRequest sysStatusRequest) {
        try {
            for(ZKRecord record : hubRecords) {
                String target = record.getURI();
                setHub(target);
                if(seenHubs.contains(record)) {
                    continue;
                }
                seenHubs.add(record);
                SysStatusResponse sysStatusResponse = this.stub.sysStatus(sysStatusRequest);
                seenHubs.clear();
                return sysStatusResponse;
            }
        } catch (StatusRuntimeException e) {
            return trySysStatus(hubRecords, sysStatusRequest);
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}
