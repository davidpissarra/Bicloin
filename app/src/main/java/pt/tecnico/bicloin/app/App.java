package pt.tecnico.bicloin.app;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;

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
    private User user;

    public App(String zooHost, String zooPort, User user) throws ZKNamingException {
        zkNaming = new ZKNaming(zooHost, zooPort);
        this.user = user;
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

    public void move(String tagName) {
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

    public void move(float latitude, float longitude) {
        try {
            user.setLocation(latitude, longitude);
            System.out.println("OK");
        } catch(InvalidUserException e) {
            System.out.println("ERRO " + e.getMessage());
        }
    }

    public void at() {
        float latitude = user.getLatitude();
        float longitude = user.getLongitude();
        String username = user.getId();
        System.out.println(username + " em https://www.google.com/maps/place/" + latitude + "," + longitude);
    }

    public void scan(Integer nStations) {
        LocateStationRequest request = LocateStationRequest
                                            .newBuilder()
                                            .setLatitude(user.getLatitude())
                                            .setLongitude(user.getLongitude())
                                            .setNStations(nStations)
                                            .build();
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            LocateStationResponse response = tryScan(hubRecords, request);
            printResponse(response);
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        
    }

    private LocateStationResponse tryScan(Collection<ZKRecord> hubRecords, LocateStationRequest request) {
        for(ZKRecord record : hubRecords) {
            setHub(record);
            try {
                LocateStationResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS).locateStation(request);
                return response;
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

    public void ping() {
        PingRequest pingRequest = PingRequest.newBuilder().build();
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
			PingResponse response = tryPing(hubRecords, pingRequest);
            printResponse(response);
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
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

    public void sysStatus() {
        SysStatusRequest sysStatusRequest = SysStatusRequest.newBuilder().build();
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            SysStatusResponse response = trySysStatus(hubRecords, sysStatusRequest);
            printResponse(response);
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
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

    public void balance() {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            BalanceRequest balanceRequest = BalanceRequest.newBuilder().setUsername(user.getId()).build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    BalanceResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .balance(balanceRequest);
                    printResponse(response);
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
    }

    public void topUp(Integer value) {
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
                    TopUpResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .topUp(topUpRequest);
                    printResponse(response);
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
    }

    public void infoStation(String abrev) {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            InfoStationRequest infoStationRequest = InfoStationRequest.newBuilder()
                                            .setStationId(abrev)
                                            .build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    InfoStationResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .infoStation(infoStationRequest);
                    printResponse(response);
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
    }

    public void bikeUp(String abrev) {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            BikeUpRequest request = BikeUpRequest
                                            .newBuilder()
                                            .setUsername(user.getId())
                                            .setUserLatitude(user.getLatitude())
                                            .setUserLongitude(user.getLongitude())
                                            .setStationId(abrev)
                                            .build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    BikeUpResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .bikeUp(request);
                    printResponse(response);
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
    }

    public void bikeDown(String abrev) {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            BikeDownRequest request = BikeDownRequest
                                            .newBuilder()
                                            .setUsername(user.getId())
                                            .setUserLatitude(user.getLatitude())
                                            .setUserLongitude(user.getLongitude())
                                            .setStationId(abrev)
                                            .build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    BikeDownResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .bikeDown(request);
                    printResponse(response);
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
    }

    private void printResponse(Message message) {
		if(message instanceof PingResponse) {
			PingResponse pingResponse = (PingResponse) message;
			System.out.println(pingResponse.getOutput());
		}
		else if(message instanceof SysStatusResponse) {
			SysStatusResponse sysStatusResponse = (SysStatusResponse) message;
			System.out.println(sysStatusResponse.getOutput());
		}
		else if(message instanceof BalanceResponse) {
			BalanceResponse balanceResponse = (BalanceResponse) message;
			System.out.println(user.getId() + " " + balanceResponse.getBalance() + " BIC");
		}
		else if(message instanceof TopUpResponse) {
			TopUpResponse topUpResponse = (TopUpResponse) message;
			System.out.println(user.getId() + " " + topUpResponse.getBalance() + " BIC");
		}
		else if(message instanceof InfoStationResponse) {
			InfoStationResponse infoStationResponse = (InfoStationResponse) message;
			String output = infoStationResponse.getName() + ", "
								+ "lat " + infoStationResponse.getLatitude() + ", "
								+ infoStationResponse.getLongitude() + " long, "
								+ infoStationResponse.getDocks() + " docas, "
								+ infoStationResponse.getReward() + " BIC prémio, "
								+ infoStationResponse.getBikes() + " bicicletas, "
								+ infoStationResponse.getBikeUpStats() + " levantamentos, "
								+ infoStationResponse.getBikeDownStats() + " devoluções, "
								+ "https://www.google.com/maps/place/"
								+ infoStationResponse.getLatitude() + ","
								+ infoStationResponse.getLongitude();
			System.out.println(output);
		}
		else if(message instanceof LocateStationResponse) {
			LocateStationResponse locateStationResponse = (LocateStationResponse) message;
            List<StationProtoMessage> nStations = locateStationResponse.getStationsList();

            nStations.forEach(station -> {
                String output = station.getAbrev() + ", "
                                    + "lat " + station.getLatitude() + ", "
                                    + station.getLongitude() + " long, "
                                    + station.getDocks() + " docas, "
                                    + station.getReward() + " BIC prémio, "
                                    + station.getBikes() + " bicicletas, a "
                                    + station.getDistance() + " metros";
                System.out.println(output);
            });
        }
        else if(message instanceof BikeUpResponse || message instanceof BikeDownResponse) {
			System.out.println("OK");
		}
	}

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}

