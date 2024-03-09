package bgu.spl.net.srv;

public class connectionsSingleton {
    private static BaseConnections<byte[]> connections;

    // Private constructor to prevent instantiation from other classes
    private connectionsSingleton() {
    }

    // Public static method that returns the instance of the class
    public static BaseConnections<byte[]> getInstance() {
        if (connections == null) {
            // If the instance is null, initialize it
            connections = new BaseConnections<byte[]>();
        }
        // Return the instance
        return connections;
    }
}
