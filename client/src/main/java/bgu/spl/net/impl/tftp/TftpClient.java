package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args)
    {
        if (args.length == 0) {
            args = new String[]{"localhost", "7777"};
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, port");
            System.exit(1);
        }

        try {
            Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            TftpEncoderDecoder endec = new TftpEncoderDecoder();
            TftpProtocol protocol = new TftpProtocol(out);

            Thread keyboardListner = new Thread(new KeyboardListner(out,endec,protocol));
            keyboardListner.start();

            Thread ServerListner = new Thread(new ServerListner(in,endec,protocol));
            ServerListner.start();

            System.out.println("Connected to the server!");

        }catch (Exception ignored){}
    }
}
