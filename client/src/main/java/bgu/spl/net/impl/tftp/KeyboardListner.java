package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.util.Scanner;
import java.net.Socket;

public class KeyboardListner implements Runnable{
    private BufferedOutputStream out;
    private Scanner keyboardScanner;
    private TftpEncoderDecoder endec;

    public KeyboardListner(BufferedOutputStream out,TftpEncoderDecoder endec)
    {
        this.out=out;
        this.endec = endec;
        this.keyboardScanner = new Scanner(System.in);
    }

    @Override
    public void run() {


            try
            {

                while (keyboardScanner.hasNextLine()) {
                    byte[] encoded = endec.encode(EncodeString(keyboardScanner.nextLine()));
                    if (encoded != null)
                    {
                        out.write(encoded);
                        out.flush();
                    }
                    else
                        System.out.println("Invalid Command");
            }}catch (Exception ignored){}
        }


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
                return resultArray;
            case("DIRQ"):
                return new byte[]{0,6};
            case("DISC"):
                return new byte[]{0,10};
        }

        return null;
    }
}
