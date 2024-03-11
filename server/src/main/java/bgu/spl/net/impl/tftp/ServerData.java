package bgu.spl.net.impl.tftp;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class ServerData {

    //INV - if a client is logged in he will be in the maps.
    private ConcurrentHashMap<String,Integer> userNameToConncetionID;
    private ConcurrentHashMap<Integer,String> ConnectionIDTOuserName;
    private LinkedList<String> fileNameLock;
    private Path folderDir;


    public ServerData() {
        this.userNameToConncetionID = new ConcurrentHashMap<String, Integer>();
        this.ConnectionIDTOuserName = new ConcurrentHashMap<Integer, String>();
        fileNameLock = new LinkedList<String>();

        Path currentDir = Paths.get("").toAbsolutePath();
        this.folderDir = currentDir.resolve("Files");

        File Folder = new File(this.folderDir.toString());
        for (File f : Folder.listFiles()) {
            this.fileNameLock.add(f.getName());

        }
    }
    public boolean isLoggedINName(String userName)
    {
        return this.ConnectionIDTOuserName.contains(userName);
    }

    public boolean isLoggedINID(int connectionID)
    {
        return this.userNameToConncetionID.contains(connectionID);
    }

    public boolean logIN(int connectionID,String userName)
    {
        if (!isLoggedINName(userName)) {
            this.userNameToConncetionID.put(userName, connectionID);
            this.ConnectionIDTOuserName.put(connectionID, userName);
            return true;
        }

        else return false;
    }

    public void logOut(int connectionID)
    {
        String userName = ConnectionIDTOuserName.get(connectionID);
        userNameToConncetionID.remove(userName);
        ConnectionIDTOuserName.remove(connectionID);
    }
    public LinkedList<Integer> getAllConnectionIds(){
        return new LinkedList<>(this.userNameToConncetionID.values());
    }

    //Reads the file data
    public byte[] readFile(String fileName) {
        try {
            int index = this.fileNameLock.indexOf(fileName);
            if (index != -1) {
                synchronized (this.fileNameLock.get(index)) {
                    return Files.readAllBytes(Paths.get(this.folderDir + "/" + fileName));
                }
            }
        }catch(Exception ignored){}
        return null;
    }

    //Creates a new file and writes data into.
    public boolean writeToFile(String fileName,LinkedList<Byte> lb)
    {
        try {
            if (this.fileNameLock.contains(fileName)) {
                return false;
            } else {
                byte[] byteArray = new byte[lb.size()];
                int i = 0;
                for (byte b : lb)
                {
                    byteArray[i] = b;
                    i++;
                }
                Files.write(Paths.get(this.folderDir + "/" + fileName), byteArray);
                this.fileNameLock.add(fileName);
                return true;
            }
        }catch (Exception ignored){}

        return false;
    }

    //Checks if the file exists
    public boolean fileExists(String filename)
    {
        String filePath = this.folderDir + "/" + filename;
        return Files.exists(Paths.get(filePath));
    }

    //Deletes the relevant file
    public boolean deleteFile(String fileName)
    {
        try {
            this.fileNameLock.remove(fileName);
            return Files.deleteIfExists(Paths.get(this.folderDir + "/" + fileName));
        }catch(Exception ignored){}
        return false;
    }

    //Returns the file name list
    public LinkedList<String> dirq()
    {
        return new LinkedList<String>(this.fileNameLock);
    }
}
