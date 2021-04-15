package pt.tecnico.bicloin.app;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.grpc.Hub.*;
import pt.tecnico.bicloin.app.domain.Tag;
import pt.tecnico.bicloin.app.domain.User;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import pt.tecnico.bicloin.app.domain.exception.InvalidLocationException;
import pt.tecnico.bicloin.app.domain.exception.InvalidUserException;

public class App implements AutoCloseable {
    private ManagedChannel channel = null;
    private HubServiceGrpc.HubServiceBlockingStub stub = null;
    private ZKNaming zkNaming;
    private int timeoutDelay = 2; //seconds
    private Map<String, Tag> tags = new HashMap<>();

    public App(String zooHost, String zooPort) throws ZKNamingException {
        zkNaming = new ZKNaming(zooHost, zooPort);
    }

    private void setHub(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = HubServiceGrpc.newBlockingStub(channel);
    }

    private Integer getInstanceNumber(ZKRecord record) {
        String path = record.getPath();
        Integer lastSlashIndex = path.lastIndexOf('/');
		return Integer.valueOf(path.substring(lastSlashIndex + 1));
    }

    public void tag(float latitude, float longitude, String tagName) {
        try {
            this.tags.put(tagName, new Tag(latitude, longitude));
            System.out.println("OK");
        } catch(InvalidLocationException e) {
            System.out.println("ERRO " + e.getMessage());
        }
    }

    public void move(String tagName, User user) {
        if(tags.containsKey(tagName)) {
            try {
                Tag tag = tags.get(tagName);
                user.setLocation(tag.getLatitude(), tag.getLongitude());
                System.out.println("OK");
            } catch(InvalidUserException e) {
                System.out.println("ERRO " + e.getMessage());
            }
        }
        else {
            System.out.println("ERRO tag não encontrada");
        }
    }

    public void move(float latitude, float longitude, User user) {
        try {
            user.setLocation(latitude, longitude);
            System.out.println("OK");
        } catch(InvalidUserException e) {
            System.out.println("ERRO " + e.getMessage());
        }
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
        for(ZKRecord record : hubRecords) {
            setHub(record);
            try {
                PingResponse pingResponse = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS).ping(pingRequest);
                return pingResponse;
            } catch (StatusRuntimeException e) {
                if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                    System.out.println("Timeout limit exceeded. Retrying to another hub.");
                }
                else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                    System.out.println("Hub instance number " + getInstanceNumber(record) + " is DOWN! Retrying to another hub.");
                }
            }
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
        for(ZKRecord record : hubRecords) {
            setHub(record);
            try {
                SysStatusResponse sysStatusResponse = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS).sysStatus(sysStatusRequest);
                return sysStatusResponse;
            } catch (StatusRuntimeException e) {
                if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                    System.out.println("Timeout limit exceeded. Retrying to another hub.");
                }
                else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                    System.out.println("Hub instance number " + getInstanceNumber(record) + " is DOWN! Retrying to another hub.");
                }
            }
        }
        return null;
    }

    public BalanceResponse balance(User user) {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            BalanceRequest balanceRequest = BalanceRequest.newBuilder().setUsername(user.getId()).build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    BalanceResponse balanceResponse = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .balance(balanceRequest);
                    return balanceResponse;
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        System.out.println("Timeout limit exceeded. Retrying to another hub.");
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        System.out.println("Hub instance number " + getInstanceNumber(record) + " is DOWN! Retrying to another hub.");
                    }
                    else {
                        System.out.println(e.getStatus().getDescription());
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    public TopUpResponse topUp(Integer value, User user) {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            TopUpRequest topUpRequest = TopUpRequest.newBuilder()
                                            .setUsername(user.getId())
                                            .setAmount(value)
                                            .setPhone(user.getPhoneNumber())
                                            .build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    TopUpResponse topUpResponse = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .topUp(topUpRequest);
                    return topUpResponse;
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        System.out.println("Timeout limit exceeded. Retrying to another hub.");
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        System.out.println("Hub instance number " + getInstanceNumber(record) + " is DOWN! Retrying to another hub.");
                    }
                    else {
                        System.out.println(e.getStatus().getDescription());
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    public InfoStationResponse infoStation(String abrev) {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            InfoStationRequest infoStationRequest = InfoStationRequest.newBuilder()
                                            .setStationId(abrev)
                                            .build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    InfoStationResponse infoStationResponse = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .infoStation(infoStationRequest);
                    return infoStationResponse;
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        System.out.println("Timeout limit exceeded. Retrying to another hub.");
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        System.out.println("Hub instance number " + getInstanceNumber(record) + " is DOWN! Retrying to another hub.");
                    }
                    else {
                        System.out.println(e.getStatus().getDescription());
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}

