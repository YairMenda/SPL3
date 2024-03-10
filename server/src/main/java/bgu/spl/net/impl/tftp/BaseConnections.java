package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.concurrent.ConcurrentHashMap;

public class BaseConnections<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connections;



    public BaseConnections()
    {
        this.connections = new ConcurrentHashMap<Integer,ConnectionHandler<T>>();
    }


    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        this.connections.put(connectionId,handler);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = this.connections.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false;
    }

    @Override
    public void disconnect(int connectionId) {
            this.connections.remove(connectionId);
    }

}
