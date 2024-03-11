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

    private boolean readyToDisconnect = false;
    public boolean connection = true;
    public Actions(BufferedOutputStream out) {
        this.out = out;
        this.currentDir = Paths.get("").toAbsolutePath();
        this.LastFileName="";
        this.bytesToWrite=new LinkedList<>();
        this.bytesToSend=new LinkedList<>();
    }

    public void act(byte[] message) {
        byte[] b = new byte[]{message[0], message[1]};
        short opcode = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));

        byte[] msgwithoutopcode = opcodeRemover(message);
        switch (opcode) {
            case 1:
                String s = new String(msgwithoutopcode, StandardCharsets.UTF_8);
                read(s.substring(0,s.length()-1));
                break;
            case 2:
                write(msgwithoutopcode);
                break;
            case 3:
                data(msgwithoutopcode);
                break;
            case 4:
                reciveAck(msgwithoutopcode);
                break;
            case 5:
                error(msgwithoutopcode);
                break;
            case 9:
                Bcast(msgwithoutopcode);
                break;
        }
    }

    public void disconnect()
    {
        this.readyToDisconnect = true;
    }

    //Remove the op code from the message
    public byte[] opcodeRemover(byte[] message) {
        byte[] result = new byte[message.length - 2];
        for (int i = 2; i < message.length; i++) {
            result[i - 2] = message[i];
        }
        return result;

    }

    //prepare the file to get data
    public void read(String filename){
        this.LastFileName=filename;
    }


    //recive data from the server
    public void data(byte[] msg) {
        byte[] b = new byte[]{msg[2], msg[3]};
        short blockNumber = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));

        byte[] s = new byte[]{msg[0], msg[1]};
        short packetSize = (short) (((short) s[0] & 0xFF) << 8 | (short) (s[1] & 0xFF));

        if (this.LastFileName != "") {
            if (packetSize == 512) {
                byte[] tempArray = DataDecoder(msg);
                for (byte p : tempArray) {
                    this.bytesToWrite.add(p);
                }
                SendAck(blockNumber);

            }
            // if it's the last packet
            else {
                try {
                    byte[] tempArray = DataDecoder(msg);
                    for (byte p : tempArray) {
                        this.bytesToWrite.add(p);
                    }

                    //try to write the data into the file
                    if (writeToFile(this.LastFileName, this.bytesToWrite)) {
                        SendAck(blockNumber);

                        System.out.println("RRQ " + this.LastFileName +" Complete");
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
        for (int i =4; i < msg.length; i++){
            if (msg[i]==0 | i == msg.length-1){
                int j=0;
                byte[] name = new byte[lb.size()];
                for(byte b: lb){
                    name[j]=b;
                    j++;
                }
                System.out.println(new String(name,StandardCharsets.UTF_8));
                lb.clear();
            }
            else {
            lb.add(msg[i]);}
        }
    }

    //prints the relevant error message recived by the server
    public void error(byte[] msg) {
        byte[] byteArray = new byte[msg.length - 3];
        for (int j = 2; j < msg.length - 1; j++) {
            byteArray[j - 2] = msg[j];
        }
        System.out.println( new String(byteArray,StandardCharsets.UTF_8));

        if (bytesToSend.size() > 0)
            bytesToSend.clear();
        if (bytesToWrite.size() > 0)
            bytesToWrite.clear();

        // if we create a file but got error in the middle, deletes the file
        if (LastFileName != "") {
            try {
                Files.deleteIfExists(Paths.get(this.currentDir + "/" + this.LastFileName));
            } catch (Exception ignored){ignored.printStackTrace();
            }
        }
        this.LastFileName = "";
        this.bytesToSend.clear();
        this.bytesToWrite.clear();
    }

    //sends ack to the server
    public void SendAck(int blockNumber) {
        short a = (short) blockNumber;
        try {
            //add the current block number to the ack array msg
            out.write(new byte[]{0, 4, (byte) (a >> 8), (byte) (a & 0xff)});
            out.flush();
        } catch (Exception ignored) {
        }
    }

    //encodes the data with the relevant block number
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

    //recives ack from the server
    public void reciveAck(byte[] msg) {
        byte[] b = new byte[]{msg[0], msg[1]};
        short blockNumber = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));
        System.out.println("ACK "+ blockNumber);

        //if the current ack is a result of a DISC operation dont send data
        if (this.readyToDisconnect) {
            this.connection = false;
        }
        else{
            //send the next data packet after the server's approval
            sendData(blockNumber + 1);
        }
    }


    //creates a new file and write the list of bytes into the file
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

    //Extract the data from the msg
    public byte[] DataDecoder(byte[] msg)
    {
        byte[] result = new byte[msg.length-4];
        for(int i=4; i< msg.length;i++ ){
            result[i-4]= msg[i];
        }
        return result;
    }

    //sends data packets to the server
    private void sendData(int blocknumber){

        //last data packet
        if (bytesToSend.size() < 512 & bytesToSend.size() > 0){
            try {
                out.write(dataEncoder(this.bytesToSend, blocknumber));
                out.flush();
                this.bytesToSend.clear();
            }catch (Exception ignored){ignored.printStackTrace();}
        }
        //regular data packet
        else if (bytesToSend.size()>511){

            LinkedList<Byte> currentData = new LinkedList<Byte>();
            for (int i = 0; i < 512; i++) {
                currentData.add(this.bytesToSend.remove());
            }
            try {
                out.write(dataEncoder(currentData, blocknumber));
                out.flush();
            }catch (Exception ignored){ignored.printStackTrace();}
        }
        else
        {
            if (this.LastFileName  != "") {
                System.out.println("WRQ " + this.LastFileName +" Complete");
                this.LastFileName = "";
            }

        }
    }


    //prepares the write file function, updates the file names and loads the bytes from the file
    public void write(byte[] msg)
    {
        this.LastFileName = byteDecoder(msg);
        try {
            byte[] byteArray = readFile(this.LastFileName);
            if (byteArray == null) {
                System.out.println("File not found in the client files");
                this.LastFileName = "";
            }
                else {
                    //loads the data from the file
                LinkedList<Byte> list = new LinkedList<Byte>();
                for (byte b : byteArray) {
                    list.add((Byte) b);
                }
                this.bytesToSend = list;
            }
        }catch (Exception ignored){}
    }

    //Read the file's data
    public byte[] readFile(String fileName) {
        try {
                    return Files.readAllBytes(Paths.get(this.currentDir + "/" + fileName));
        }catch(Exception ignored){}
        return null;}

    //Decode the msg into a string
    public String byteDecoder(byte[] msg)
    {
        return new String(msg, StandardCharsets.UTF_8).substring(0,msg.length-1);
    }

    //Prints the Bcast msg
    public void Bcast(byte[] msg){
        byte[] s = new byte[]{msg[0]};
        int action = (short) (((short) s[0] & 0xFF) << 8);
        byte[] result = new byte[msg.length-2];
        for (int i = 1; i<msg.length-1;i++){
            result[i-1] = msg[i];
        }
        if (action == 0){
            System.out.println("BCAST del "+ new String(result,StandardCharsets.UTF_8));
        }
        else {
            System.out.println("BCAST add " + new String(result,StandardCharsets.UTF_8));
        }

    }
}