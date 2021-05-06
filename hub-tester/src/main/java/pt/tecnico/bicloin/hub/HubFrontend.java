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
import pt.tecnico.bicloin.hub.domain.AppUser;

import pt.tecnico.bicloin.hub.domain.exception.InvalidLocationException;
import pt.tecnico.bicloin.hub.domain.exception.InvalidUserException;

public class HubFrontend implements AutoCloseable {
    private ManagedChannel channel = null;
    private HubServiceGrpc.HubServiceBlockingStub stub = null;
    private Integer instance;
    private ZKNaming zkNaming;
    private int timeoutDelay = 2; //seconds
    private Map<String, Tag> tags = new HashMap<>();
    private AppUser user;

    public HubFrontend(ZKNaming zkNaming, AppUser user, String path) throws ZKNamingException {
        this.zkNaming = zkNaming;
        this.user = user;
        setHub(path);
    }

    public void setHub(String path) throws ZKNamingException {
        ZKRecord record = zkNaming.lookup(path);
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
            Tag tag = tags.get(tagName);
            return move(tag.getLatitude(), tag.getLongitude());
        }
        else {
            return "ERRO tag não encontrada";
        }
    }

    public String move(float latitude, float longitude) {
        try {
            user.setLocation(latitude, longitude);
            return user.getId()
                    + " em " + "https://www.google.com/maps/place/"
                    + user.getLatitude() + "," + user.getLongitude();
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
            LocateStationResponse response = stub.withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS).locateStation(request);
            return responseOutput(response);
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String ping() {
        try {
            PingRequest request = PingRequest.newBuilder().build();
            return responseOutput(stub.ping(request));
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String sysStatus() {
        SysStatusRequest sysStatusRequest = SysStatusRequest.newBuilder().build();
        try {
            SysStatusResponse sysStatusResponse = stub
                                                    .withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                                    .sysStatus(sysStatusRequest);
            return responseOutput(sysStatusResponse); 
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String balance() {
        try {
            BalanceRequest balanceRequest = BalanceRequest.newBuilder().setUsername(user.getId()).build();
            BalanceResponse response = stub
                                        .withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                        .balance(balanceRequest);
            return responseOutput(response);
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String topUp(Integer value) {
        try {
            TopUpRequest topUpRequest = TopUpRequest.newBuilder()
                                            .setUsername(user.getId())
                                            .setAmount(value)
                                            .setPhone(user.getPhoneNumber())
                                            .build();
            TopUpResponse response = stub
                                        .withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                        .topUp(topUpRequest);
            return responseOutput(response);
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String infoStation(String abrev) {
        try {
            InfoStationRequest infoStationRequest = InfoStationRequest.newBuilder()
                                            .setStationId(abrev)
                                            .build();
            InfoStationResponse response = stub
                                            .withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                            .infoStation(infoStationRequest);
            return responseOutput(response);
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String bikeUp(String abrev) {
        try {
            BikeUpRequest request = BikeUpRequest
                                            .newBuilder()
                                            .setUsername(user.getId())
                                            .setUserLatitude(user.getLatitude())
                                            .setUserLongitude(user.getLongitude())
                                            .setStationId(abrev)
                                            .build();
            BikeUpResponse response = stub
                                        .withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                        .bikeUp(request);
            return responseOutput(response);
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
    }

    public String bikeDown(String abrev) {
        try {
            BikeDownRequest request = BikeDownRequest
                                            .newBuilder()
                                            .setUsername(user.getId())
                                            .setUserLatitude(user.getLatitude())
                                            .setUserLongitude(user.getLongitude())
                                            .setStationId(abrev)
                                            .build();
            BikeDownResponse response = stub
                                            .withDeadlineAfter(timeoutDelay, TimeUnit.SECONDS)
                                            .bikeDown(request);
            return responseOutput(response); 
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Code.DEADLINE_EXCEEDED) {
                return "ERRO Tempo de conexão com o Hub expirado.";
            }
            else if(e.getStatus().getCode() == Code.UNAVAILABLE) {
                return "ERRO Servidor Hub não se encontra ativo.";
            }
            else {
                return e.getStatus().getDescription();
            }
        }
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
