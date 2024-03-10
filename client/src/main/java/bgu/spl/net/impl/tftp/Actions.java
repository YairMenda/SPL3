package bgu.spl.net.impl.tftp;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class Actions {
    private String LastFileName;
    private LinkedList<Byte> bytesToSend;
    private LinkedList<Byte> bytesToWrite;
    private Path currentDir;
    private BufferedOutputStream out;

    public Actions(BufferedOutputStream out) {
        this.out = out;
        Path currentDir = Paths.get("").toAbsolutePath();
        this.LastFileName="";
    }

    public void act(byte[] message) {
        byte[] b = new byte[]{message[0], message[1]};
        short opcode = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));

        byte[] msgwithoutopcode = opcodeRemover(message);

        switch (opcode) {

            case 3:
                data(msgwithoutopcode);
                break;
            case 4:
                reciveAck(msgwithoutopcode);
                break;
            case 5:
                error(msgwithoutopcode);
                break;
        }
    }

    public byte[] opcodeRemover(byte[] message) {
        byte[] result = new byte[message.length - 2];
        for (int i = 2; i < message.length; i++) {
            result[i - 2] = message[i];
        }
        return result;

    }

    public void data(byte[] msg) {
        byte[] b = new byte[]{msg[4], msg[5]};
        short blockNumber = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));

        byte[] s = new byte[]{msg[2], msg[3]};
        short packetSize = (short) (((short) s[0] & 0xFF) << 8 | (short) (s[1] & 0xFF));

        if (this.LastFileName != "") {
            if (packetSize == 512) {
                byte[] tempArray = DataDecoder(msg).getBytes();
                for (byte p : tempArray) {
                    this.bytesToWrite.add(p);
                }
                SendAck(blockNumber);
            } else {
                try {
                    byte[] tempArray = DataDecoder(msg).getBytes();
                    for (byte p : tempArray) {
                        this.bytesToWrite.add(p);
                    }
                    if (writeToFile(this.LastFileName, this.bytesToWrite)) {
                        SendAck(blockNumber);
//
//                    byte[] msgNotifcation = ("WRQ " + this.LastFileName + " complete").getBytes();
//                    LinkedList<Byte> lb = new LinkedList<Byte>();
//                    for (byte p : msgNotifcation)
//                        lb.add(p);
//                    connections.send(connectionID, dataEncoder(lb,1));

                        this.LastFileName = "";
                        this.bytesToWrite.clear();
                    } else {
                        System.out.println("Failed to read This File ");
                        this.LastFileName = "";
                        this.bytesToWrite.clear();
                    }
                } catch (Exception ignored) {}
            }
        }
        else {
            printDIRQ(msg);
            SendAck(blockNumber);
        }
    }

    // recives an undecoded data packet and needs to print it
    public void printDIRQ(byte[] msg)
    {
        LinkedList<Byte> lb = new LinkedList<>();
        for (int i =6; i < msg.length; i++){
            if (msg[i]==0){
                int j=0;
                byte[] name = new byte[lb.size()];
                for(byte b: lb){
                    name[j]=b;
                }
                System.out.println(name.toString());
                lb.clear();
            }
            else {
            lb.add(msg[i]);}
        }
    }
    public void error(byte[] msg) {

        byte[] byteArray = new byte[msg.length - 5];
        for (int j = 4; j < msg.length - 1; j++) {
            byteArray[j - 4] = msg[j];
        }
        System.out.println(byteArray.toString());

        if (bytesToSend.size() > 0)
            bytesToSend.clear();
        if (bytesToWrite.size() > 0)
            bytesToWrite.clear();

        if (LastFileName != "") {
            try {
                Files.deleteIfExists(Paths.get(this.currentDir + "/" + this.LastFileName));
            } catch (Exception ignored) {
            }
        }
        this.LastFileName = "";
    }

    public void SendAck(int blockNumber) {
        short a = (short) blockNumber;
        try {
            out.write(new byte[]{0, 4, (byte) (a >> 8), (byte) (a & 0xff)});
            out.flush();
        } catch (Exception ignored) {
        }
    }

    private byte[] dataEncoder(List<Byte> lb, int blockNumber) {
        byte[] DataEncoded = new byte[6 + lb.size()];
        byte[] blockNumberPacket = new byte[]{(byte) ((blockNumber >> 8) & 0xff), (byte) (blockNumber & 0xff)};
        byte[] packetSize = new byte[]{(byte) ((lb.size() >> 8) & 0xff), (byte) (lb.size() & 0xff)};

        DataEncoded[0] = 0;
        DataEncoded[1] = 3;
        DataEncoded[2] = packetSize[0];
        DataEncoded[3] = packetSize[1];
        DataEncoded[4] = blockNumberPacket[0];
        DataEncoded[5] = blockNumberPacket[1];

        int i = 6;
        for (byte b : lb) {
            DataEncoded[i] = b;
            i++;
        }
        return DataEncoded;
    }

    public void reciveAck(byte[] msg) {
        byte[] b = new byte[]{msg[0], msg[1]};
        short blockNumber = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));
        System.out.println("ACK "+ blockNumber);
        sendData(blockNumber + 1);
    }


    public boolean writeToFile(String fileName, LinkedList<Byte> lb) {
        String filePath = this.currentDir + "/" + fileName;
        try {
            if (Files.exists(Paths.get(filePath))) {
                return false;
            } else {
                byte[] byteArray = new byte[lb.size()];
                int i = 0;
                for (byte b : lb) {
                    byteArray[i] = b;
                    i++;
                }
                Files.write(Paths.get(this.currentDir + "/" + fileName), byteArray);
                System.out.println("File Created - " + fileName);
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }
    public String DataDecoder(byte[] msg)
    {
        return new String(msg, StandardCharsets.UTF_8).substring(4,msg.length);
    }

    private void sendData(int blocknumber){
        if (bytesToSend.size() < 512 & bytesToSend.size() > 0){
            try {
                out.write(dataEncoder(this.bytesToSend, blocknumber));
                this.bytesToSend.clear();
            }catch (Exception ignored){}
        }
        else if (bytesToSend.size()>511){
            LinkedList<Byte> currentData = new LinkedList<Byte>();
            for (int i = 0; i < 512; i++) {
                currentData.add(this.bytesToSend.remove());
            }
            try {
                out.write(dataEncoder(currentData, blocknumber));
            }catch (Exception ignored){}
        }
        else
        {
            if (this.LastFileName  != "") {

                this.LastFileName = "";
            }

        }
    }
}