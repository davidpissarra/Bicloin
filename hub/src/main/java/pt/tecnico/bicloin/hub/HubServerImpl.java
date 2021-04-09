package pt.tecnico.bicloin.hub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.bicloin.hub.grpc.Hub;
import pt.tecnico.rec.grpc.Rec;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import static io.grpc.Status.INTERNAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.rpc.Status;

public class HubServerImpl extends HubServiceGrpc.HubServiceImplBase {
    
    private Integer instance;
    private ZKNaming zkNaming;
    private ZKRecord record;
    List<ZKRecord> seenRecords = new ArrayList<>();

    public HubServerImpl(Integer instance, ZKNaming zkNaming) {
        super();
        this.instance = instance;
        this.zkNaming = zkNaming;
    }

    @Override
    public void ping(Hub.PingRequest request, StreamObserver<Hub.PingResponse> responseObserver) {
        String output = "Hub instance number " + instance + " is UP.";
        Hub.PingResponse response = Hub.PingResponse.newBuilder().setOutput(output).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sysStatus(Hub.SysStatusRequest request, StreamObserver<Hub.SysStatusResponse> responseObserver) {
        try {
            Collection<ZKRecord> recRecords = zkNaming.listRecords("/grpc/bicloin/rec");
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");

            String output = getOutput(recRecords, hubRecords);

            Hub.SysStatusResponse response = Hub.SysStatusResponse.newBuilder().setOutput(output).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        
        } catch(ZKNamingException e) {
            responseObserver.onError(INTERNAL.withDescription("Internal error.").asRuntimeException());
        }
    }

    private String getOutput(Collection<ZKRecord> recRecords, Collection<ZKRecord> hubRecords) {
        String output = "";
        output = pingRecs(recRecords, output);
        seenRecords.clear();
        output = pingHubs(hubRecords, output);
        seenRecords.clear();
        return output;
    }

    private String pingRecs(Collection<ZKRecord> recRecords, String output) {
        try {
            for(ZKRecord record : recRecords) {
                System.out.println(output);
                this.record = record;
                String target = record.getURI();
                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                RecServiceGrpc.RecServiceBlockingStub stub = RecServiceGrpc.newBlockingStub(channel);
                Rec.PingRequest recRequest = Rec.PingRequest.newBuilder().build();

                seenRecords.add(record);
                Rec.PingResponse recResponse = stub.ping(recRequest);
                output += recResponse.getOutput() + "\n";
            }
        } catch(StatusRuntimeException e) {
            System.out.println("Unreachable rec record.");
            String path = record.getPath();
            Integer lastSlashIndex = path.lastIndexOf("/");
            Integer instance = Integer.valueOf(path.substring(lastSlashIndex + 1));
            output += "Rec instance number " + instance + " is DOWN.\n";
        }
        return output;
    }

    private String pingHubs(Collection<ZKRecord> hubRecords, String output) {
        try {
            for(ZKRecord record : hubRecords) {
                if(seenRecords.contains(record)) {
                    continue;
                }
                this.record = record;
                String target = record.getURI();
                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                HubServiceGrpc.HubServiceBlockingStub stub = HubServiceGrpc.newBlockingStub(channel);
                Hub.PingRequest hubRequest = Hub.PingRequest.newBuilder().build();
                seenRecords.add(record);
                Hub.PingResponse hubResponse = stub.ping(hubRequest);
                output += hubResponse.getOutput() + "\n";
            }
        } catch(StatusRuntimeException e) {
            System.out.println("Unreachable hub record.");
            String path = record.getPath();
            Integer lastSlashIndex = path.lastIndexOf("/");
            Integer instance = Integer.valueOf(path.substring(lastSlashIndex + 1));
            output += "Hub instance number " + instance + " is DOWN.\n";
            return pingHubs(hubRecords, output);
        }
        return output;
    }
}
