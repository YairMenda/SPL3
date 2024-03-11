package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.sql.SQLOutput;
import java.util.Scanner;
import java.net.Socket;

public class KeyboardListner implements Runnable{
    private BufferedOutputStream out;
    private Scanner keyboardScanner;
    private TftpEncoderDecoder endec;

    private TftpProtocol protocol;

    public KeyboardListner(BufferedOutputStream out,TftpEncoderDecoder endec,TftpProtocol protocol)
    {
        this.out=out;
        this.endec = endec;
        this.keyboardScanner = new Scanner(System.in);
        this.protocol=protocol;
    }

    @Override
    public void run() {
            try
            {
                while (!protocol.shouldTerminate()) {
                    if (System.in.available() > 0){
                        // encodes the input from the client
                    byte[] encoded = endec.encode(EncodeString(keyboardScanner.nextLine()));
                    if (encoded != null)
                    {
                        //write the encoded input to server
                        out.write(encoded);
                        out.flush();
                    }
                    else
                        System.out.println("Invalid Command");
                    }
                }
            }catch (Exception ignored){}

        this.keyboardScanner.close();
        }

    //Encodes the string given by the client, and creates the relevant byte array
    public byte[] EncodeString(String s)
    {
        String opcode = s.split(" ")[0];
        byte[] resultArray;
        byte[] withoutOPcode;
        int i;
        switch (opcode) {
            case ("LOGRQ"):
                withoutOPcode = s.substring(6).getBytes();
                resultArray = new byte[withoutOPcode.length + 3];
                resultArray[0] = 0;
                resultArray[1] = 7;
                resultArray[resultArray.length - 1] = 0;
                i = 2;
                for (byte b : withoutOPcode){
                    resultArray[i] = b;
                    i++;
                }
                return resultArray;
            case("RRQ"):
                withoutOPcode = s.substring(4).getBytes();
                resultArray = new byte[withoutOPcode.length + 3];
                resultArray[0] = 0;
                resultArray[1] = 1;
                resultArray[resultArray.length - 1] = 0;
                i = 2;
                for (byte b : withoutOPcode){
                    resultArray[i] = b;
                    i++;
                }
                protocol.process(resultArray);
                return resultArray;
            case("DELRQ"):
                withoutOPcode = s.substring(6).getBytes();
                resultArray = new byte[withoutOPcode.length + 3];
                resultArray[0] = 0;
                resultArray[1] = 8;
                resultArray[resultArray.length - 1] = 0;
                i = 2;
                for (byte b : withoutOPcode){
                    resultArray[i] = b;
                    i++;
                }
                return resultArray;
            case("WRQ"):
                withoutOPcode = s.substring(4).getBytes();
                resultArray = new byte[withoutOPcode.length + 3];
                resultArray[0] = 0;
                resultArray[1] = 2;
                resultArray[resultArray.length - 1] = 0;
                i = 2;
                for (byte b : withoutOPcode){
                    resultArray[i] = b;
                    i++;
                }
                protocol.process(resultArray);
                return resultArray;
            case("DIRQ"):
                return new byte[]{0,6};
            case("DISC"):{
                protocol.disconnect();
                return new byte[]{0,10};
            }
        }

        return null;
    }
}
