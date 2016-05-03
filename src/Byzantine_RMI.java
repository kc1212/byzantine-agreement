import java.rmi.RemoteException;

public interface Byzantine_RMI extends java.rmi.Remote {
    void handleMsg(Byzantine.MsgType type, int r, int w) throws RemoteException;
}
