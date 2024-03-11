package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.*;

import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.LinkedList;
import java.util.List;

public class Action {

    private ServerData sd;
    private int connectionID;

    private BaseConnections connections;

    private String[] errorStringArray;
    //private Path folderDir;
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

        String error0 = "Not defined, see error message (if any).";
        String error1 = "File not found – RRQ DELRQ of non-existing file.";
        String error2 = "Access violation – File cannot be written, read or deleted.";
        String error3 = "Disk full or allocation exceeded – No room in disk.";
        String error4 =  "Illegal TFTP operation – Unknown Opcode.";
        String error5 =  "File already exists – File name exists on WRQ.";
        String error6 = "User not logged in – Any opcode received before Login completes.";
        String error7 = "User already logged in – Login username already connected.";

        this.errorStringArray = new String[]{error0,error1,error2,error3,error4,error5,error6,error7};

        //Path currentDir = Paths.get("").toAbsolutePath();
        //Path serverDir = currentDir.getParent().getParent().getParent().getParent();
        //this.folderDir = serverDir.resolve("Files");
    }
    public void act(byte[] message) {
       byte[] b = new byte[]{message[0],message[1]};
       short opcode = (short)(((short)b[0] & 0xFF) << 8 | (short)(b[1] & 0xFF));

       byte[] msgwithoutopcode = opcodeRemover(message);

        System.out.println("opcode " +opcode);
       if (serverDataSingleton.getInstance().isLoggedINID(this.connectionID) | opcode == 7) {
           switch (opcode) {
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
       } else
           error(6);
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

    //login function
    public void login(byte[] message)
    {
        String userName = byteDecoder(message);
        if(!sd.isLoggedINName(userName)) {
            //adds the client to the server data
            if(sd.logIN(connectionID, userName))
            {
                SendAck(0);
            }
            else error(7);
        }
        else
            error(7);

    }

    public void disc()
    {
        //removes the client from the server data
        sd.logOut(connectionID);
        SendAck(0);
        //disconnects the connection handler
        connections.disconnect(connectionID);
    }

    //Ready to recive data to write
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

    //Ready to send data from the server files
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

    //sends datat to the client
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
//
//                byte[] msgNotifcation = ("RRQ " + this.LastFileName + " complete").getBytes();
//                LinkedList<Byte> lb = new LinkedList<Byte>();
//                for (byte b : msgNotifcation)
//                    lb.add(b);
//                connections.send(connectionID, dataEncoder(lb,1));
                this.LastFileName = "";
            }

        }
    }

    //Encodes the bytes list with the relevant block number
    private byte[] dataEncoder(List<Byte> lb, int blockNumber){
        byte[] DataEncoded = new byte[6 + lb.size()];
        byte[] blockNumberPacket = new byte[]{(byte) ((blockNumber >> 8) & 0xff), (byte)(blockNumber & 0xff)};
        byte[] packetSize = new byte[]{(byte) ((lb.size() >> 8) & 0xff) , (byte)(lb.size() & 0xff)};

        DataEncoded[0] = 0;
        DataEncoded[1] = 3;
        DataEncoded[2] = packetSize[0];
        DataEncoded[3] = packetSize[1];
        DataEncoded[4] = blockNumberPacket[0];
        DataEncoded[5] = blockNumberPacket[1];

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

    //Decode the data from the msg
    public byte[] DataDecoder(byte[] msg)
    {
        byte[] result = new byte[msg.length-4];
        for(int i=4; i< msg.length;i++ ){
            result[i-4]= msg[i];
        }
        return result;
    }

    //Sends the relevant error to the client
    public void error(int errorIndex)
    {
        byte[] byteArray = new byte[this.errorStringArray[errorIndex].getBytes().length + 5];
        byteArray[0] = 0;
        byteArray[1] = 5;
        byteArray[2] = 0;
        byteArray[3] = (byte)errorIndex;

        for (int j = 4; j < byteArray.length - 1;j++)
        {
          byteArray[j] = this.errorStringArray[errorIndex].getBytes()[j-4];
        }
        byteArray[byteArray.length - 1] = 0;
        connections.send(connectionID,byteArray);
    }

    //sends ack to the client
    public void SendAck(int blockNumber)
    {
        short a = (short) blockNumber;
        connections.send(connectionID,new byte []{0,4,(byte)( a >> 8 & 0xff) , (byte)( a & 0xff )});
    }

    //recives data from the client
    public void data(byte[] msg) {
        byte[] b = new byte[]{msg[2], msg[3]};
        short blockNumber = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));
        byte[] s = new byte[]{msg[0], msg[1]};
        short packetSize = (short) (((short) s[0] & 0xFF) << 8 | (short) (s[1] & 0xFF));

        //regular data packet
        if (packetSize == 512) {
            byte[] tempArray = DataDecoder(msg);
            for (byte p : tempArray) {
                this.bytesToWrite.add(p);
            }
            SendAck(blockNumber);

        }
        //last data packet
        else {
            try {
                byte[] tempArray = DataDecoder(msg);
                for (byte p : tempArray) {
                    this.bytesToWrite.add(p);
                }
                if (this.sd.writeToFile(this.LastFileName, this.bytesToWrite)) {
                    SendAck(blockNumber);

                    //broadcast the action to the clients
                    Bcast(1);
                    this.LastFileName = "";
                    this.bytesToWrite.clear();
                } else {
                    error(2);
                    this.LastFileName = "";
                    this.bytesToWrite.clear();
                }
            } catch (Exception ignored) {ignored.printStackTrace();
            }
        }
    }

    //Broadcast the action to the clients
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

    //Recives ack from the client
    public void reciveAck(byte[] msg)
    {
        byte[] b = new byte[]{msg[0],msg[1]};
        short blockNumber = (short)(((short)b[0] & 0xFF) << 8 | (short)(b[1] & 0xFF));
        sendData(blockNumber + 1);
    }

    //Delets the file if exists
    public void delete(byte[] msg)
    {
        this.LastFileName = byteDecoder(msg);
        if (this.sd.fileExists(this.LastFileName))
        {
            if (this.sd.deleteFile(this.LastFileName)) {
                System.out.println("delete successful");
                SendAck(0);
                Bcast(0);
            }
            else
                error(2);

        }
        else
            error(1);
    }

    // Return the files names
    public void dirq()
    {
        LinkedList<String> fileNames = this.sd.dirq();
        for (String f : fileNames)
        {

            byte[] bArray = f.getBytes();
            for (byte b : bArray)
                this.bytesToSend.add(b);

            this.bytesToSend.add((byte)0);
        }

        //this.bytesToSend.removeLast();
        sendData(1);

    }

}