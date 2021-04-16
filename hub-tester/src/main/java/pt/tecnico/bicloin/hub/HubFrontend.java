package pt.tecnico.bicloin.hub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.tecnico.bicloin.hub.grpc.Hub.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.domain.Tag;
import pt.tecnico.bicloin.hub.domain.User;

import pt.tecnico.bicloin.hub.domain.exception.InvalidLocationException;
import pt.tecnico.bicloin.hub.domain.exception.InvalidUserException;

public class HubFrontend implements AutoCloseable {
    private ManagedChannel channel = null;
    private HubServiceGrpc.HubServiceBlockingStub stub = null;
    private Integer instance;
    private ZKNaming zkNaming;
    private int timeoutDelay = 2; //seconds
    private Map<String, Tag> tags = new HashMap<>();
    private User user;

    public HubFrontend(ZKNaming zkNaming, User user) throws ZKNamingException {
        this.zkNaming = zkNaming;
        this.user = user;
    }

    public void setHub(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = HubServiceGrpc.newBlockingStub(channel);
        setInstance(record);
    }

    public Integer getInstance() {
        return this.instance;
    }

    private void setInstance(ZKRecord record) {
        String path = record.getPath();
        Integer lastSlashIndex = path.lastIndexOf('/');
		this.instance = Integer.valueOf(path.substring(lastSlashIndex + 1));
    }

    public String tag(float latitude, float longitude, String tagName) {
        try {
            this.tags.put(tagName, new Tag(latitude, longitude));
            return "OK";
        } catch(InvalidLocationException e) {
            return "ERRO " + e.getMessage();
        }
    }

    public String move(String tagName) {
        if(tags.containsKey(tagName)) {
            try {
                Tag tag = tags.get(tagName);
                user.setLocation(tag.getLatitude(), tag.getLongitude());
                return "OK";
            } catch(InvalidUserException e) {
                return "ERRO " + e.getMessage();
            }
        }
        else {
            return "ERRO tag não encontrada";
        }
    }

    public String move(float latitude, float longitude) {
        try {
            user.setLocation(latitude, longitude);
            return "OK";
        } catch(InvalidUserException e) {
            return "ERRO " + e.getMessage();
        }
    }

    public String at() {
        float latitude = user.getLatitude();
        float longitude = user.getLongitude();
        String username = user.getId();
        return username + " em https://www.google.com/maps/place/" + latitude + "," + longitude;
    }

    public String scan(Integer nStations) {
        LocateStationRequest request = LocateStationRequest
                                            .newBuilder()
                                            .setLatitude(user.getLatitude())
                                            .setLongitude(user.getLongitude())
                                            .setNStations(nStations)
                                            .build();
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            LocateStationResponse response = tryScan(hubRecords, request);
            return responseOutput(response);
        } catch (ZKNamingException e) {
            System.out.println("ERRO ZKNAMING EXCEPTION");
        }
        return null;
    }

    private LocateStationResponse tryScan(Collection<ZKRecord> hubRecords, LocateStationRequest request) {
        for(ZKRecord record : hubRecords) {
            setHub(record);
            try {
                LocateStationResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS).locateStation(request);
                return response;
            } catch (StatusRuntimeException e) {
                if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                    System.out.println("ERRO Timeout limit exceeded. Retrying to another hub.");
                }
                else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                    System.out.println("ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.");
                }
            }
        }
        return null;
    }

    public String ping(ZKRecord record) {
        setHub(record);
        PingRequest request = PingRequest.newBuilder().build();
        try {
            return responseOutput(stub.ping(request));
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Timeout limit exceeded. Retrying to another hub.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String ping() {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            PingRequest request = PingRequest.newBuilder().build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    return responseOutput(stub.ping(request));
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        return "ERRO Timeout limit exceeded. Retrying to another hub.";
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        return "ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.";
                    }
                    else {
                        return e.getStatus().getDescription();
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    public String sysStatus() {
        SysStatusRequest sysStatusRequest = SysStatusRequest.newBuilder().build();
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            SysStatusResponse response = trySysStatus(hubRecords, sysStatusRequest);
            return responseOutput(response);
        } catch (ZKNamingException e) {
            System.out.println("ERRO ZKNAMING EXCEPTION");
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
                    System.out.println("ERRO Timeout limit exceeded. Retrying to another hub.");
                }
                else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                    System.out.println("ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.");
                }
            }
        }
        return null;
    }

    public String balance() {
        try {
            Collection<ZKRecord> hubRecords = zkNaming.listRecords("/grpc/bicloin/hub");
            BalanceRequest balanceRequest = BalanceRequest.newBuilder().setUsername(user.getId()).build();
            for(ZKRecord record : hubRecords) {
                setHub(record);
                try {
                    BalanceResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                        .balance(balanceRequest);
                    return responseOutput(response);
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        return " ERRO Timeout limit exceeded. Retrying to another hub.";
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        return "ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.";
                    }
                    else {
                       return e.getStatus().getDescription();
                    }
                }
            }
        } catch (ZKNamingException e) {
            return "ERRO ZKNAMING EXCEPTION";
        }
        return null;
    }

    public String topUp(Integer value) {
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
                    return responseOutput(response);
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                       return "ERRO Timeout limit exceeded. Retrying to another hub.";
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        return "ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.";
                    }
                    else {
                        return e.getStatus().getDescription();
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    public String infoStation(String abrev) {
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
                    return responseOutput(response);
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        return "ERRO Timeout limit exceeded. Retrying to another hub.";
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        return "ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.";
                    }
                    else {
                        return e.getStatus().getDescription();
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    public String bikeUp(String abrev) {
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
                    return responseOutput(response);
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        return "ERRO Timeout limit exceeded. Retrying to another hub.";
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        return "ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.";
                    }
                    else {
                        return e.getStatus().getDescription();
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    public String bikeDown(String abrev) {
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
                    return responseOutput(response);
                } catch (StatusRuntimeException e) {
                    if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                        return "ERRO Timeout limit exceeded. Retrying to another hub.";
                    }
                    else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                        return "ERRO Hub instance number " + getInstance() + " is DOWN! Retrying to another hub.";
                    }
                    else {
                        return e.getStatus().getDescription();
                    }
                }
            }
        } catch (ZKNamingException e) {
            System.out.println("ZKNAMING EXCEPTION");
        }
        return null;
    }

    public String printHelp(String path) {
        try {
            File help = new File(path);
            Scanner scanner = new Scanner(help);
            String output = "";
            while(scanner.hasNext()) {
                output += scanner.nextLine() + "\n";
            }
            scanner.close();
            return output;
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private String responseOutput(Message message) {
		if(message instanceof PingResponse) {
			PingResponse pingResponse = (PingResponse) message;
            return pingResponse.getOutput();
		}
		else if(message instanceof SysStatusResponse) {
			SysStatusResponse sysStatusResponse = (SysStatusResponse) message;
			return sysStatusResponse.getOutput();
		}
		else if(message instanceof BalanceResponse) {
			BalanceResponse balanceResponse = (BalanceResponse) message;
			return user.getId() + " " + balanceResponse.getBalance() + " BIC";
		}
		else if(message instanceof TopUpResponse) {
			TopUpResponse topUpResponse = (TopUpResponse) message;
			return user.getId() + " " + topUpResponse.getBalance() + " BIC";
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
			return output;
		}
		else if(message instanceof LocateStationResponse) {
			LocateStationResponse locateStationResponse = (LocateStationResponse) message;
            List<StationProtoMessage> nStations = locateStationResponse.getStationsList();
            String output = "";
            for(StationProtoMessage station: nStations) {
                output += station.getAbrev() + ", "
                                    + "lat " + station.getLatitude() + ", "
                                    + station.getLongitude() + " long, "
                                    + station.getDocks() + " docas, "
                                    + station.getReward() + " BIC prémio, "
                                    + station.getBikes() + " bicicletas, a "
                                    + station.getDistance() + " metros\n";
            }
            return output.substring(0, output.length() - 1);
        }
        else if(message instanceof BikeUpResponse || message instanceof BikeDownResponse) {
			return "OK";
		}
        return null;
	}

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}
