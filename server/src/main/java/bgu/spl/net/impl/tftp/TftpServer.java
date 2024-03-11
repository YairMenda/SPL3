package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.Server;

public class TftpServer{
    //TODO: Implement this

    public static void main(String[] args) {

        // generates thread per client with the relevant factories
        Server.threadPerClient(
                7777, //port
                () -> new TftpProtocol(), //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
        ).serve();

    }


}
