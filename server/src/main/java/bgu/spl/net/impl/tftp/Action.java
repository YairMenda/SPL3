package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.BaseConnections;
import bgu.spl.net.srv.ServerData;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class Action {

    private ServerData sd;
    private int connectionID;

    public Action(ServerData sd, int connectionID)
    {
        this.sd = sd;
        this.connectionID = connectionID;
    }
    public byte[] act(byte[] message) {
       byte[] b = new byte[]{message[0],message[1]};
       short opcode = (short)(((short)b[0]) << 8 | (short)(b[1]));

       byte[] msgwithoutopcode = opcodeRemover(message);
       switch (opcode){
           case 1:
               return read(msgwithoutopcode);
               break;
           case 2:
               return write(msgwithoutopcode);
               break;
           case 3:
               return data(msgwithoutopcode);
               break;

           case 6:
               return dirq(msgwithoutopcode);
               break;

           case 7:
               return login(msgwithoutopcode);
               break;
           case 8:
               return delete(msgwithoutopcode);
               break;
           case 9:
               return bcast(msgwithoutopcode);
               break;
           case 10:
               return disc();
               break;
       }
        return null;
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
    public byte[] login(byte[] message)
    {
        String userName = byteDecoder(message);
        if(!sd.isLoggedINName(userName)) {
            sd.logIN(connectionID, userName);
            return ack(0);
        }
        else
            return error(7);
    }

    public byte[] disc()
    {
        sd.logOut(connectionID);
        return ack(0);
    }
    public byte[] write(byte[] msg)
    {
        String fileName = byteDecoder(msg);
        if (!fileExists(fileName))
        {
            //Last action - how to get the data packets
        }
        else
            return error(5);
    }

    public byte[] read(byte[] msg)
    {
        String fileName = byteDecoder(msg);
        if (!fileExists(fileName))
        {
            //Last action - how to get the data packets
        }
        else
            return error(1);
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

    public boolean fileExists(String filename)
    {
        Path currentDir = Paths.get("").toAbsolutePath();
        Path serverDir = currentDir.getParent().getParent().getParent().getParent();
        Path folderDir = serverDir.resolve("Files");

        String filePath = folderDir + "/" + filename;

        return Files.exists(Paths.get(filePath));
    }



}