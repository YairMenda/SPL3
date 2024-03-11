package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.net.Socket;

public class TftpProtocol {

    private boolean shouldTerminate = false;
    private Actions action;



    public TftpProtocol(BufferedOutputStream out) {
        this.shouldTerminate = false;
        this.action = new Actions(out);
    }
    public void process(byte[] message) {
        // TODO implement this
        action.act(message);

    }

    public boolean shouldTerminate() {
        return !action.connection;
}

    public void disconnect()
    {
        action.disconnect();
    }
    /*
    public void connect()
    {
        this.shouldTerminate = true;

    }*/
}
