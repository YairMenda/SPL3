package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;

public class ServerData {

    //INV - if a client is logged in he will be in the maps.
    private ConcurrentHashMap<String,Integer> userNameToConncetionID;
    private ConcurrentHashMap<Integer,String> ConnectionIDTOuserName;

    public ServerData()
    {
        this.userNameToConncetionID = new ConcurrentHashMap<String,Integer>();
        this.ConnectionIDTOuserName = new ConcurrentHashMap<Integer,String>();
    }
    public boolean isLoggedINName(String userName)
    {
        return this.userNameToConncetionID.contains(userName);
    }

    public boolean isLoggedINID(int connectionID)
    {
        return this.ConnectionIDTOuserName.contains(connectionID);
    }

    public void logIN(int connectionID,String userName)
    {
        this.userNameToConncetionID.put(userName,connectionID);
        this.ConnectionIDTOuserName.put(connectionID,userName);
    }

    public boolean logOut(int connectionID)
    {
        String userName = ConnectionIDTOuserName.get(connectionID);
        userNameToConncetionID.remove(userName);
        ConnectionIDTOuserName.remove(connectionID);
    }
}
