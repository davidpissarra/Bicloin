package pt.tecnico.rec;

import com.google.protobuf.Message;

import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.Rec.ReadResponse;
import pt.tecnico.rec.grpc.Rec.WriteResponse;
import io.grpc.Status;

import java.util.List;

public class MainThread extends Thread {

    private List<Message> responseCollector;
    private Integer quorumThreshold;
    private Integer lastSize;
    private Message response;
    private Boolean timeout;

    public MainThread(List<Message> responseCollector, Integer quorumThreshold) {
        this.responseCollector = responseCollector;
        this.quorumThreshold = quorumThreshold;
        this.lastSize = 0;
        this.timeout = false;
    }

    public Message getMessage() {
        return this.response;
    }

    public Boolean getTimeout() {
        return this.timeout;
    }

    public void run() throws StatusRuntimeException {
        try {
            synchronized(responseCollector) {
                while(responseCollector.size() <= quorumThreshold) {
                    responseCollector.wait(2 * 1000);
                    if(lastSize == responseCollector.size()) {
                        this.timeout = true;
                        return;
                    }
                    this.lastSize++;
                }
                System.out.println("Quorum aceite!");
            }
            response = null;
            Integer tag = -1;
            if(responseCollector.get(0) instanceof WriteResponse) {
                this.response = responseCollector.get(0);
                return;
            }
            for(Message message: responseCollector) {
                if(message instanceof ReadResponse) {
                    ReadResponse m = (ReadResponse) message;
                    if(m.getTag() > tag) {
                        this.response = m;
                        tag = m.getTag();
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Caught exception: " + e.toString());
        }
    }
}
