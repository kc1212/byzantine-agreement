import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Byzantine
        extends UnicastRemoteObject
        implements Byzantine_RMI {

    private int round;
    private boolean decided;
    private int v;
    private List<Tuple<Integer, Integer>> msgsN;
    private List<Tuple<Integer, Integer>> msgsP;

    private final int n, f;
    private final List<String> addrs;

    private enum MsgType {
        NOTIFICATION,
        PROPOSAL
    }

    static class MajTally {
        public final int maj;
        public final long tally;
        public MajTally(int maj, long tally) {
            this.maj = maj;
            this.tally = tally;
        }
        public void println() {
            System.out.printf("Maj: %d, Tally: %l\n", maj, tally);
        }
    }

    public Byzantine(int _n, int _f, int _v, List<String> _addrs) throws RemoteException {
        round = 0;
        decided = false;
        n = _n;
        f = _f;
        v = _v;
        addrs = new ArrayList<>(_addrs);
        msgsN = new ArrayList<>();
        msgsP = new ArrayList<>();

        assert addrs.size() == n;
    }

    private void await(List<Tuple<Integer, Integer>> msgs, Integer count)
            throws InterruptedException {
        // FIXME this does not work, Java parameters are passed by value
        while (true) {
            synchronized (msgs) {
                if (msgs.stream().filter(p -> p.r == round).count() > count)
                    break;
            }
            Thread.sleep(100);
        }
    }

    public void run()
            throws RemoteException, MalformedURLException, InterruptedException {
        Random rn = new Random();
        for (;;) {
            /* NOTIFICATION PHASE */

            bcast(MsgType.NOTIFICATION, round, v);
            await(msgsN, n - f);

            /* PROPOSAL PHASE */

            MajTally proposalRes = getMajTally(msgsN, round);

            if (proposalRes.tally > (n + f) / 2)  {
                bcast(MsgType.PROPOSAL, round, proposalRes.maj);
            } else {
                bcast(MsgType.PROPOSAL, round, -1);
            }

            if (decided)
                break;

            await(msgsP, n - f);

            /* DECISION PHASE */

            MajTally decisionRes = getMajTally(msgsP, round);

            if (decisionRes.tally > f) {
                v = decisionRes.maj;
                if (decisionRes.tally > 3*f) {
                    decide(v);
                    decided = true;
                }
            } else {
                v = rn.nextInt() % 2;
            }

            prepareNewRound();
            round++;
        }
    }

    public void recvNotification(int r, int w) throws RemoteException {
        synchronized (msgsN) {
            msgsN.add(new Tuple<>(r, w));
        }
    }

    public void recvProposal(int r, int w) throws RemoteException {
        synchronized (msgsP) {
            msgsP.add(new Tuple<>(r, w));
        }
    }

    private void bcast(MsgType type, int r, int w)
            throws RemoteException, MalformedURLException {
        for (String addr : addrs) {
            try {
                send(type, addr, r, w);
            } catch (NotBoundException e) {
                System.err.printf("Sending failed on %s\n", addr);
                e.printStackTrace();
            }
        }
    }

    private static void send(MsgType type, String dest, int r, int w)
            throws RemoteException, MalformedURLException, NotBoundException {

        Byzantine_RMI remote = (Byzantine_RMI) Naming.lookup(dest);

        if (type == MsgType.NOTIFICATION) {
            System.out.printf("Sending (N, %d, %d) to %s\n", r, w, dest);
            remote.recvNotification(r, w);

        } else if (type == MsgType.PROPOSAL) {
            System.out.printf("Sending (P, %d, %d) to %s\n", r, w, dest);
            remote.recvProposal(r, w);

        } else {
            throw new RemoteException("Invalid Msg Type!");
        }
    }


    private synchronized void prepareNewRound() {
        msgsN = filterMsgList(msgsN, round);
        msgsP = filterMsgList(msgsP, round);
    }

    private static void decide(int w) {
        System.out.printf("I decided on %d!\n", w);
    }

    private static List<Tuple<Integer, Integer>> filterMsgList(List<Tuple<Integer, Integer>> msgs, Integer r) {
        return msgs.stream().filter(p -> p.r != r).collect(Collectors.toList());
    }

    // TODO one could make a more general version of this instead of 0 or 1
    private static MajTally getMajTally(List<Tuple<Integer, Integer>> wrs, Integer r) {
        long tally0 = getTallyOf(wrs, 0, r);
        long tally1 = getTallyOf(wrs, 1, r);

        int maj = 0;
        long tally = tally0;
        if (tally0 < tally1) {
            maj = 1;
            tally = tally1;
        }

        return new MajTally(maj, tally);
    }

    private static long getTallyOf(List<Tuple<Integer, Integer>> wrs, Integer w, Integer r) {
        return wrs.stream().filter(p -> p.w == w && p.r == r).count();
    }
}
