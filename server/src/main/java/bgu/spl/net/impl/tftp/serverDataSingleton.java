package bgu.spl.net.impl.tftp;

import bgu.spl.net.impl.tftp.ServerData;

public class serverDataSingleton {
    private static ServerData serverData;

    // Private constructor to prevent instantiation from other classes
    private serverDataSingleton() {
    }

    // Public static method that returns the instance of the class
    public static ServerData getInstance() {
        if (serverData == null) {
            // If the instance is null, initialize it
            serverData = new ServerData();
        }
        // Return the instance
        return serverData;
    }
}
