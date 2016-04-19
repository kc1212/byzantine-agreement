import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Byzantine
        extends UnicastRemoteObject
        implements Byzantine_RMI {

    private int round;
    private boolean decided;
    private int n, f, v;
    private List<Tuple<Integer, Integer>> notificationMsgs;
    private List<Tuple<Integer, Integer>> proposalMsgs;

    private enum MsgType {
        NOTIFICATION,
        PROPOSAL
    }

    public Byzantine(int _n, int _f, int _v) throws RemoteException {
        round = 0;
        decided = false;
        n = _n;
        f = _f;
        v = _v;
        notificationMsgs = new ArrayList<>();
        proposalMsgs = new ArrayList<>();
    }

    public void run() throws RemoteException, InterruptedException {
        for (;;) {
            bcast(MsgType.NOTIFICATION, round, v);

            // wait for notification messages
            while (true) {
                synchronized (notificationMsgs) {
                    if (notificationMsgs.stream().filter(p -> p.r == round).count() > n - f)
                        break;
                }
                Thread.sleep(100);
            }

            long count0 = notificationMsgs.stream().filter(p -> p.w == 0 && p.r == round).count();
            long count1 = notificationMsgs.stream().filter(p -> p.w == 1 && p.r == round).count();

            int maj = 0;
            if (count0 < count1) {
                maj = 1;
            }

            if ((maj == 0 && count0 > (n + f) / 2) || (maj == 1 && count1 > (n + f) / 2)) {
                bcast(MsgType.PROPOSAL, round, maj);
            } else {
                bcast(MsgType.PROPOSAL, round, -1);
            }

            if (decided)
                break;

            // TODO
        }
    }

    private void bcast(MsgType type, int r, int w) throws RemoteException {
        if (type == MsgType.NOTIFICATION) {
            // TODO
        } else if (type == MsgType.PROPOSAL) {
            // TODO
        } else {
            throw new RemoteException("Invalid Msg Type!");
        }
    }

    public void recvNotification() throws RemoteException {
    }

    public void recvProposal() throws RemoteException {
    }
}
