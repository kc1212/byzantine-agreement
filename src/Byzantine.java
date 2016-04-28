import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    static class MajTally {
        public final int maj;
        public final long tally;
        public MajTally(int maj, long tally) {
            this.maj = maj;
            this.tally = tally;
        }
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

    private void await(List<Tuple<Integer, Integer>> msgs, Integer count) throws InterruptedException {
        // FIXME this does not work, Java parameters are passed by value
        while (true) {
            synchronized (msgs) {
                if (msgs.stream().filter(p -> p.r == round).count() > count)
                    break;
            }
            Thread.sleep(100);
        }
    }

    public void run() throws RemoteException, InterruptedException {
        Random rn = new Random();
        for (;;) {
            /* NOTIFICATION PHASE */

            bcast(MsgType.NOTIFICATION, round, v);
            await(notificationMsgs, n - f);

            /* PROPOSAL PHASE */

            MajTally proposalRes = getMajTally(notificationMsgs, round);

            if (proposalRes.tally > (n + f) / 2)  {
                bcast(MsgType.PROPOSAL, round, proposalRes.maj);
            } else {
                bcast(MsgType.PROPOSAL, round, -1);
            }

            if (decided)
                break;

            await(proposalMsgs, n - f);

            /* DECISION PHASE */

            MajTally decisionRes = getMajTally(proposalMsgs, round);

            if (decisionRes.tally > f) {
                v = decisionRes.maj;
                if (decisionRes.tally > 3*f) {
                    decide(v);
                    decided = true;
                }
            } else {
                v = rn.nextInt() % 2;
            }
            round++;
        }
    }

    private void decide(int w) {
        System.out.printf("I decided on %d!\n", w);
    }

    // TODO one could make a more general version of this instead of 01
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
