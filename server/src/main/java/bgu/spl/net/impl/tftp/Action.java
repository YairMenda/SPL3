package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.*;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Action {

    private ServerData sd;
    private int connectionID;

    private Connections<byte[]> connections;

    private String[] errorStringArray;
    private Path folderDir;
    private Boolean DataWriteORread = false; // false = read
    private String LastFileName;
    private LinkedList<Byte> bytesToSend;

    private LinkedList<Byte> bytesToWrite;
    public Action(int connectionID)
    {
        this.sd = serverDataSingleton.getInstance();
        this.connectionID = connectionID;
        this.connections = connectionsSingleton.getInstance();
        this.bytesToSend = new LinkedList<Byte>();
        this.bytesToWrite = new LinkedList<Byte>();

        String error0 = "0500Not defined, see error message (if any).0";
        String error1 = "0501File not found – RRQ DELRQ of non-existing file.0";
        String error2 = "0502Access violation – File cannot be written, read or deleted.0";
        String error3 = "0503Disk full or allocation exceeded – No room in disk.0";
        String error4 =  "0504Illegal TFTP operation – Unknown Opcode.0";
        String error5 =  "0505File already exists – File name exists on WRQ.0";
        String error6 = "0506User not logged in – Any opcode received before Login completes.0";
        String error7 = "0507User already logged in – Login username already connected.0";

        this.errorStringArray = new String[]{error0,error1,error2,error3,error4,error5,error6,error7};

        Path currentDir = Paths.get("").toAbsolutePath();
        Path serverDir = currentDir.getParent().getParent().getParent().getParent();
        this.folderDir = serverDir.resolve("Files");
    }
    public void act(byte[] message) {
       byte[] b = new byte[]{message[0],message[1]};
       short opcode = (short)(((short)b[0] & 0xFF) << 8 | (short)(b[1] & 0xFF));

       byte[] msgwithoutopcode = opcodeRemover(message);

       switch (opcode){
           case 1:
               read(msgwithoutopcode);
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

           case 6:
               dirq();
               break;

           case 7:
               login(msgwithoutopcode);
               break;
           case 8:
               delete(msgwithoutopcode);
               break;
           case 10:
               disc();
               break;
       }
    }

    /**
     * The function removes the opcode from the array
     * @param message
     * @return
     */
    public byte[] opcodeRemover(byte[] message)
    {
        byte[] result = new byte[message.length - 2];
        for (int i = 2; i < message.length ; i++)
        {
            result[i-2] = message[i];
        }
        return result;

    }
    public void login(byte[] message)
    {
        String userName = byteDecoder(message);
        if(!sd.isLoggedINName(userName)) {
            sd.logIN(connectionID, userName);
            SendAck(0);
        }
        else
            error(7);

    }

    public void disc()
    {
        sd.logOut(connectionID);
        SendAck(0);
    }
    public void write(byte[] msg)
    {
         this.LastFileName = byteDecoder(msg);
        if (!this.sd.fileExists(this.LastFileName))
        {
            this.DataWriteORread = true;
            SendAck(0);
        }
        else
            error(5);
    }

    public void read(byte[] msg)
    {
        this.LastFileName = byteDecoder(msg);
        try {
                byte[] byteArray = this.sd.readFile(this.LastFileName);
                if (byteArray == null)
                    error(1);

                LinkedList<Byte> list = new LinkedList<Byte>();
                for(byte b : byteArray){
                    list.add((Byte) b);
                }
                this.bytesToSend = list;
                sendData(1);
            }catch (Exception ignored){}
    }

    private void sendData(int blocknumber){
        if (bytesToSend.size() < 512 & bytesToSend.size() > 0){
            connections.send(connectionID,dataEncoder(this.bytesToSend,blocknumber));
            this.bytesToSend.clear();
        }
        else if (bytesToSend.size()>511){
            LinkedList<Byte> currentData = new LinkedList<Byte>();
            for (int i = 0; i < 512; i++) {
                currentData.add(this.bytesToSend.remove());
            }
            connections.send(connectionID, dataEncoder(currentData, blocknumber));

        }

        else
        {
            if (this.LastFileName  != "") {
                String clientNotification = "10RRQ " + this.LastFileName + " complete0";
                connections.send(connectionID, clientNotification.getBytes());
                this.LastFileName = "";
            }

        }
    }


    private byte[] dataEncoder(List<Byte> lb, int blockNumber){
        byte[] DataEncoded = new byte[6 + lb.size()];
        DataEncoded[0] = 0;
        DataEncoded[1] = 3;

        if (lb.size() > 255) {
            DataEncoded[2] = (byte)(lb.size() - 255);
            DataEncoded[3] = (byte)255;
        }
        else
        {
            DataEncoded[2] =  0;
            DataEncoded[3] = (byte)(lb.size());
        }

        DataEncoded[0] = 0;
        DataEncoded[0] = (byte) blockNumber;

        int i = 6;
        for (byte b : lb)
        {
            DataEncoded[i] = b;
            i++;
        }

        return DataEncoded;


    }



    /**
     * The function converts the byte array into string
     * @param msg
     * @return
     */
    public String byteDecoder(byte[] msg)
    {
        return new String(msg, StandardCharsets.UTF_8).substring(0,msg.length-1);
    }

    public String DataDecoder(byte[] msg)
    {
        return new String(msg, StandardCharsets.UTF_8).substring(4,msg.length);
    }

    public void error(int errroIndex)
    {
        connections.send(connectionID,this.errorStringArray[errroIndex].getBytes());
    }

    public void SendAck(int blockNumber)
    {
        short a = (short) blockNumber;
        connections.send(connectionID,new byte []{0,4,(byte)( a >> 8) , (byte)( a & 0xff )});
    }

    public void data(byte[] msg)
    {
        byte[] b = new byte[]{msg[4],msg[5]};
        short blockNumber = (short)(((short)b[0] & 0xFF) << 8 | (short)(b[1] & 0xFF));

        byte[] s = new byte[]{msg[2],msg[3]};
        short packetSize = (short)(((short)s[0] & 0xFF) << 8 | (short)(s[1] & 0xFF));

        if (DataWriteORread)
        {
            if (packetSize == 512) {
                byte[] tempArray = DataDecoder(msg).getBytes();
                for (byte p: tempArray)
                {
                    this.bytesToWrite.add(p);
                }
                SendAck(blockNumber);
                }
            }

            else{
                try {
                    byte[] tempArray = DataDecoder(msg).getBytes();
                    for (byte p: tempArray)
                    {
                        this.bytesToWrite.add(p);
                    }
                    if (this.sd.writeToFile(this.LastFileName, this.bytesToWrite))
                    {
                        SendAck(blockNumber);
                        String clientNotification = "10WRQ" + this.LastFileName + "comlete0";
                        connections.send(connectionID,clientNotification.getBytes());
                        Bcast(1);
                        this.LastFileName = "";
                        this.bytesToWrite.clear();
                    }
                    else {
                        error(2);
                        this.LastFileName = "";
                        this.bytesToWrite.clear();
                    }
                } catch (Exception ignored) {
                }
            }
        }

    public void Bcast(int index)
    {
        byte[] tocast = new byte[4 + this.LastFileName.length()];
        tocast[0] = 0;
        tocast[1] = 9;
        tocast[2] = (byte)index;
        int j = 3;
        for (int i = 0; i < this.LastFileName.length();i++)
        {
            tocast[i + j] = (byte) this.LastFileName.charAt(i);
        }
        tocast[tocast.length-1] = 0;

        for (Integer id : this.sd.getAllConnectionIds())
            connections.send(id,tocast);
    }

    public void reciveAck(byte[] msg)
    {
        byte[] b = new byte[]{msg[2],msg[3]};
        short blockNumber = (short)(((short)b[0] & 0xFF) << 8 | (short)(b[1] & 0xFF));
        sendData(blockNumber + 1);
    }

    public void delete(byte[] msg)
    {
        this.LastFileName = byteDecoder(msg);
        if (this.sd.fileExists(this.LastFileName))
        {
            if (this.sd.deleteFile(this.LastFileName)) {
                SendAck(0);
                Bcast(0);
            }
            else
                error(2);

        }
        else
            error(1);
    }

    public void dirq()
    {
        LinkedList<String> fileNames = this.sd.dirq();
        for (String f : fileNames)
        {
            f = f + "0";
            byte[] bArray = f.getBytes();
            for (byte b : bArray)
                this.bytesToSend.add(b);
        }

        sendData(0);

    }

}