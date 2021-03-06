package pt.tecnico.rec;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.RecServiceGrpc;
import pt.tecnico.rec.grpc.Rec.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

public class RecFrontend implements AutoCloseable {

    private ManagedChannel channel;
    private RecServiceGrpc.RecServiceBlockingStub stub;
    private Collection<ZKRecord> records;
    private int quorumThresholdRead;
    private int quorumThresholdWrite;
    private int numberReads;
    private int numberWrites;

    public RecFrontend(ZKNaming zkNaming) throws ZKNamingException {
        this.records = zkNaming.listRecords("/grpc/bicloin/rec");
        this.numberReads = 0;
        this.numberWrites = 0;
        updateQuorumThreshold();
    }

    public void updateQuorumThreshold() {
        if(this.records.size() < 5) {
            this.quorumThresholdRead = (int) (this.records.size() / 2);
            this.quorumThresholdWrite = (int) (this.records.size() / 2);
        }
        else {
            if(this.numberReads == 0 || this.numberWrites == 0) {
                this.quorumThresholdRead = (int) (this.records.size() / 2);
                this.quorumThresholdWrite = (int) (this.records.size() / 2);
            }
            else {
                double readRatio = (double) this.numberReads / (this.numberReads + this.numberWrites);
                this.quorumThresholdWrite = (int) (this.records.size() * readRatio);
                if(records.size() % 2 == 0) {
                    this.quorumThresholdRead = this.records.size() - this.quorumThresholdWrite;
                }
                else {
                    this.quorumThresholdRead = this.records.size() - this.quorumThresholdWrite - 1;
                }
            }
        }
        if(this.quorumThresholdWrite >= this.records.size() - 1) {
            this.quorumThresholdWrite = this.records.size() - 2;
            this.quorumThresholdRead = 1;
        }
    }

    public void setRec(ZKRecord record) {
        if(channel != null) {
            this.channel.shutdown();
        }
        String target = record.getURI();
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = RecServiceGrpc.newBlockingStub(channel);
    }

    public Collection<ZKRecord> getRecords() {
        return this.records;
    }

    public String ping(ZKRecord record) {
        setRec(record);
        PingRequest request = PingRequest.newBuilder().build();
        String output = stub.ping(request).getOutput();
        this.channel.shutdown();
        return output;
    }

    public ReadResponse read(String registerName) throws StatusRuntimeException {
        try {
            this.numberReads++;
            this.updateQuorumThreshold();
            ReadRequest request = ReadRequest
                                        .newBuilder()
                                        .setRegisterName(registerName)
                                        .build();
            List<Message> responseCollector = Collections.synchronizedList(new ArrayList<>());
            MainThread mainThread = new MainThread(responseCollector, quorumThresholdRead);
            mainThread.start();
            List<RecThread> threads = new ArrayList<>();
            for(ZKRecord record: this.records) {
                threads.add(new RecThread(responseCollector, record, request));
            }
            for(RecThread thread: threads) {
                thread.start();
            }
            mainThread.join();
            for(RecThread thread: threads) {
                thread.shutDownChannel();
            }
            if(mainThread.getTimeout() == true) {
                throw new StatusRuntimeException(Status.DEADLINE_EXCEEDED);
            }
            return (ReadResponse) mainThread.getMessage();
        } catch (InterruptedException e) {
            System.out.println("Caught exception: " + e.toString());
            throw new StatusRuntimeException(Status.INTERNAL);
        }
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

    
    public WriteResponse write(String registerName, Any value, Boolean initRec) throws StatusRuntimeException {
        try {
            if(initRec == false) {
                this.numberWrites++;
                this.updateQuorumThreshold();
            }
            WriteRequest request = WriteRequest
                                    .newBuilder()
                                    .setRegisterName(registerName)
                                    .setValue(value)
                                    .build();
            List<Message> responseCollector = Collections.synchronizedList(new ArrayList<>());
            MainThread mainThread = new MainThread(responseCollector, quorumThresholdWrite);
            mainThread.start();
            List<RecThread> threads = new ArrayList<>();
            for(ZKRecord record: this.records) {
                threads.add(new RecThread(responseCollector, record, request));
            }
            for(RecThread thread: threads) {
                thread.start();
            }
            mainThread.join();
            for(RecThread thread: threads) {
                thread.shutDownChannel();
            }
            return (WriteResponse) mainThread.getMessage();
        } catch (InterruptedException e) {
            System.out.println("Caught exception: " + e.toString());
            throw new StatusRuntimeException(Status.INTERNAL);
        }
    }

    public Integer writeBalance(String registerName, Integer balance, Boolean initRec) throws StatusRuntimeException, InvalidProtocolBufferException {
        Balance balanceMessage = Balance.newBuilder().setBalance(balance).build();
        Any value = Any.pack(balanceMessage);
        return write(registerName, value, initRec).getRegisterValue().unpack(Balance.class).getBalance();
    }

    public Integer writeBikes(String registerName, Integer bikes, Boolean initRec) throws StatusRuntimeException, InvalidProtocolBufferException {
        Bikes bikesMessage = Bikes.newBuilder().setBikes(bikes).build();
        Any value = Any.pack(bikesMessage);
        return write(registerName, value, initRec).getRegisterValue().unpack(Bikes.class).getBikes();
    }

    public Integer writeBikeUpStats(String registerName, Integer bikeUpStats, Boolean initRec) throws StatusRuntimeException, InvalidProtocolBufferException {
        BikeUpStats bikeUpStatsMessage = BikeUpStats.newBuilder().setBikeUpStats(bikeUpStats).build();
        Any value = Any.pack(bikeUpStatsMessage);
        return write(registerName, value, initRec).getRegisterValue().unpack(BikeUpStats.class).getBikeUpStats();
    }

    public Integer writeBikeDownStats(String registerName, Integer bikeDownStats, Boolean initRec) throws StatusRuntimeException, InvalidProtocolBufferException {
        BikeDownStats bikeDownStatsMessage = BikeDownStats.newBuilder().setBikeDownStats(bikeDownStats).build();
        Any value = Any.pack(bikeDownStatsMessage);
        return write(registerName, value, initRec).getRegisterValue().unpack(BikeDownStats.class).getBikeDownStats();
    }

    public Boolean writeIsBikedUp(String registerName, Boolean isBikedUp, Boolean initRec) throws StatusRuntimeException, InvalidProtocolBufferException {
        IsBikedUp isBikedUpMessage = IsBikedUp.newBuilder().setIsBikedUp(isBikedUp).build();
        Any value = Any.pack(isBikedUpMessage);
        return write(registerName, value, initRec).getRegisterValue().unpack(IsBikedUp.class).getIsBikedUp();
    }

    public void clean() throws StatusRuntimeException, InvalidProtocolBufferException {
        for(ZKRecord record: records) {
            setRec(record);
            CleanRequest request = CleanRequest.newBuilder().build();
            stub.clean(request);
        }
    }

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }
}
