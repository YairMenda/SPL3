package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BaseConnections;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ServerData;

import java.util.List;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;

    private int connectionID;

    private Connections<byte[]> connections;

    private Action action;
    @Override
    public void process(byte[] message) {
        // TODO implement this

       action.act(message);

    }

    @Override
    public void start(int connectionId, Connections<byte[]> connections, ServerData sd) {
        this.shouldTerminate = false;
        this.connectionID = connectionId;
        this.connections = connections;
        this.action = new Action(sd,connectionID,connections);
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    } 


    
}
