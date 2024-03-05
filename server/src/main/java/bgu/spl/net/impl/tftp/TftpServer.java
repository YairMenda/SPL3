package bgu.spl.net.impl.tftp;

import bgu.spl.net.impl.echo.EchoProtocol;
import bgu.spl.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl.net.srv.BaseConnections;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.Server;

public class TftpServer{
    //TODO: Implement this
    private BaseConnections<byte[]> connections;
    public TftpServer()
    {
        this.connections = new BaseConnections<>();
    }
    public static void main(String[] args) {

        // you can use any server...
        Server.threadPerClient(
                7777, //port
                () -> new TftpProtocol(), //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
        ).serve();

    }


}
