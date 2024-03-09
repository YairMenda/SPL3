package bgu.spl.net.srv;

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
    public ServerData()
    {
        this.userNameToConncetionID = new ConcurrentHashMap<String,Integer>();
        this.ConnectionIDTOuserName = new ConcurrentHashMap<Integer,String>();
        fileNameLock = new LinkedList<String>();

        Path currentDir = Paths.get("").toAbsolutePath();
        //Path serverDir = currentDir.getParent().getParent().getParent().getParent();
        System.out.println(currentDir);
        //System.out.println(serverDir);
        this.folderDir = currentDir.resolve("Files");
        System.out.println(folderDir);
    }
    public boolean isLoggedINName(String userName)
    {
        return this.userNameToConncetionID.contains(userName);
    }

    public boolean isLoggedINID(int connectionID)
    {
        return this.userNameToConncetionID.contains(connectionID);
    }

    public void logIN(int connectionID,String userName)
    {
        this.userNameToConncetionID.put(userName,connectionID);
        this.ConnectionIDTOuserName.put(connectionID,userName);

        System.out.println(userName + " " + connectionID);
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

    public boolean fileExists(String filename)
    {
        String filePath = this.folderDir + "/" + filename;
        return Files.exists(Paths.get(filePath));
    }

    public boolean deleteFile(String fileName)
    {
        try {
            int index = this.fileNameLock.indexOf(fileName);
                synchronized (this.fileNameLock.get(index)) {
                    this.fileNameLock.remove(index);
                    return Files.deleteIfExists(Paths.get(this.folderDir + "/" + fileName));
            }
        }catch(Exception ignored){}
        return false;
    }

    public LinkedList<String> dirq()
    {
        return new LinkedList<String>(this.fileNameLock);
    }
}
