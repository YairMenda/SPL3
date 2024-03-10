package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;

    private int connectionID;
    private Action action;
    @Override
    public void process(byte[] message) {
        // TODO implement this

        System.out.println("Process");
       action.act(message);

    }

    @Override
    public void start(int connectionId) {
        this.shouldTerminate = false;
        this.connectionID = connectionId;
        this.action = new Action(connectionID);
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    } 


    
}
