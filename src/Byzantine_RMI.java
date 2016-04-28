import java.rmi.RemoteException;

public interface Byzantine_RMI extends java.rmi.Remote {
    class Tuple<X, Y> {
        public final X r;
        public final Y w;
        public Tuple(X r, Y w) {
            this.r = r;
            this.w = w;
        }
    }
    void recvNotification(int r, int w) throws RemoteException;
    void recvProposal(int r, int w) throws RemoteException;

}
