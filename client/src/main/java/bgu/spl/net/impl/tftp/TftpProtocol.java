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

        System.out.println("Process");
        action.act(message);

    }

    public boolean shouldTerminate() {
        return shouldTerminate;
}}
