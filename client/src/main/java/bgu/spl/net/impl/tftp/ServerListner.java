package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerListner implements Runnable{
    private BufferedInputStream in;
    private TftpEncoderDecoder endec;
    private TftpProtocol protocol;
    private volatile boolean connected = true;



    public ServerListner(BufferedInputStream in,TftpEncoderDecoder endec,TftpProtocol protocol)
    {
        this.in=in;
        this.endec=endec;
        this.protocol=protocol;
    }
    @Override
    public void run() {

        try{
            int read;

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                byte[] nextMessage = endec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    System.out.println(nextMessage.toString());
                    protocol.process(nextMessage);
                }
            }

        }catch (Exception ignored){}}

        public void close() throws IOException {
            connected = false;
        }


}





