package pt.tecnico.rec;

import java.util.Collection;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class RecFrontend implements AutoCloseable {

    private ManagedChannel channel;
    private RecServiceGrpc.RecServiceBlockingStub stub;
    private Integer instance;
    private ZKNaming zkNaming;

    public RecFrontend(ZKNaming zkNaming, String path) throws ZKNamingException {
        this.zkNaming = zkNaming;
        setRec(zkNaming.lookup(path));
    }

    public Integer getInstance() {
        return instance;
    }

    public void setRec(ZKRecord record) {
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = RecServiceGrpc.newBlockingStub(channel);
        this.instance = getInstanceNumber(record);
    }

    private Integer getInstanceNumber(ZKRecord record) {
        String path = record.getPath();
        Integer lastSlashIndex = path.lastIndexOf('/');
		return Integer.valueOf(path.substring(lastSlashIndex + 1));
    }

    public Collection<ZKRecord> getHubRecords() throws ZKNamingException {
        return zkNaming.listRecords("/grpc/bicloin/hub");
    }

    public Collection<ZKRecord> getRecRecords() throws ZKNamingException {
        return zkNaming.listRecords("/grpc/bicloin/rec");
    }

    public String ping() {
        PingRequest request = PingRequest.newBuilder().build();
        return stub.ping(request).getOutput();
    }

    public ReadResponse read(String registerName) throws StatusRuntimeException {
        ReadRequest request = ReadRequest.newBuilder().setRegisterName(registerName).build();
        return stub.read(request);
    }

    public Integer readBalance(String registerName) throws StatusRuntimeException, InvalidProtocolBufferException {
        return read(registerName).getRegisterValue().unpack(Balance.class).getBalance();
    }

    public Integer readBikes(String registerName) throws StatusRuntimeException, InvalidProtocolBufferException {
        return read(registerName).getRegisterValue().unpack(Bikes.class).getBikes();
    }

    public Integer readBikeUpStats(String registerName) throws StatusRuntimeException, InvalidProtocolBufferException {
        return read(registerName).getRegisterValue().unpack(BikeUpStats.class).getBikeUpStats();
    }

    public Integer readBikeDownStats(String registerName) throws StatusRuntimeException, InvalidProtocolBufferException {
        return read(registerName).getRegisterValue().unpack(BikeDownStats.class).getBikeDownStats();
    }

    public Boolean readIsBikedUp(String registerName) throws StatusRuntimeException, InvalidProtocolBufferException {
        return read(registerName).getRegisterValue().unpack(IsBikedUp.class).getIsBikedUp();
    }

    
    public WriteResponse write(String registerName, Any value) throws StatusRuntimeException {
        WriteRequest request = WriteRequest
                                    .newBuilder()
                                    .setRegisterName(registerName)
                                    .setValue(value)
                                    .build();
        return stub.write(request);
    }

    public Integer writeBalance(String registerName, Integer balance) throws StatusRuntimeException, InvalidProtocolBufferException {
        Balance balanceMessage = Balance.newBuilder().setBalance(balance).build();
        Any value = Any.pack(balanceMessage);
        return write(registerName, value).getRegisterValue().unpack(Balance.class).getBalance();
    }

    public Integer writeBikes(String registerName, Integer bikes) throws StatusRuntimeException, InvalidProtocolBufferException {
        Bikes bikesMessage = Bikes.newBuilder().setBikes(bikes).build();
        Any value = Any.pack(bikesMessage);
        return write(registerName, value).getRegisterValue().unpack(Bikes.class).getBikes();
    }

    public Integer writeBikeUpStats(String registerName, Integer bikeUpStats) throws StatusRuntimeException, InvalidProtocolBufferException {
        BikeUpStats bikeUpStatsMessage = BikeUpStats.newBuilder().setBikeUpStats(bikeUpStats).build();
        Any value = Any.pack(bikeUpStatsMessage);
        return write(registerName, value).getRegisterValue().unpack(BikeUpStats.class).getBikeUpStats();
    }

    public Integer writeBikeDownStats(String registerName, Integer bikeDownStats) throws StatusRuntimeException, InvalidProtocolBufferException {
        BikeDownStats bikeDownStatsMessage = BikeDownStats.newBuilder().setBikeDownStats(bikeDownStats).build();
        Any value = Any.pack(bikeDownStatsMessage);
        return write(registerName, value).getRegisterValue().unpack(BikeDownStats.class).getBikeDownStats();
    }

    public Boolean writeIsBikedUp(String registerName, Boolean isBikedUp) throws StatusRuntimeException, InvalidProtocolBufferException {
        IsBikedUp isBikedUpMessage = IsBikedUp.newBuilder().setIsBikedUp(isBikedUp).build();
        Any value = Any.pack(isBikedUpMessage);
        return write(registerName, value).getRegisterValue().unpack(IsBikedUp.class).getIsBikedUp();
    }

    public void clean() throws StatusRuntimeException, InvalidProtocolBufferException {
        CleanRequest request = CleanRequest.newBuilder().build();
        stub.clean(request);
    }

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}
