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

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    private int round;
    private boolean decided;
    private int v;
    private List<Triple<MsgType, Integer, Integer>> msgs;

    private final int id, n, f;
    private final List<String> addrs;
    private final FailureType type;

    public enum MsgType {
        NOTIFICA,
        PROPOSAL
    }

    public enum FailureType {
        NOFAILURE,
        OMISSION,
        RANDOM_SIMPLE, // broadcast the same random value to all nodes
        RANDOM_COMPLEX, // different random values are sent to individual nodes
        BOTH // omit message half of the time and use RANDOM_COMPLEX when sending messages
    }

    static class Triple<X, Y, Z> {
        public final X t;
        public final Y r;
        public final Z w;
        public Triple(X t, Y r, Z w) {
            this.t = t;
            this.r = r;
            this.w = w;
        }
    }

    static class MajTally {
        public final int maj;
        public final long tally;
        public MajTally(int maj, long tally) {
            this.maj = maj;
            this.tally = tally;
        }
    }

    public Byzantine(int id, int n, int f, int v, List<String> addrs, FailureType type) throws RemoteException {
        this.round = 0;
        this.decided = false;
        this.id = id;
        this.n = n;
        this.f = f;
        this.v = v;
        this.addrs = new ArrayList<>(addrs);
        this.msgs = new ArrayList<>();
        this.type = type;

        assert addrs.size() == n;
        System.out.printf("%d initiated with v = %d, type = %s\n", id, v, type.toString());
    }

    public void run()
            throws RemoteException, MalformedURLException, InterruptedException {
        Random rn = new Random();
        for (;;) {
            // artificial delay up to 1s
            Thread.sleep(rn.nextInt(1000));

            /* NOTIFICATION PHASE */

            if (type == FailureType.OMISSION) {

            } else if (type == FailureType.RANDOM_SIMPLE) {
                bcast(MsgType.NOTIFICA, rn.nextInt(2));

            } else if (type == FailureType.RANDOM_COMPLEX) {
                randBcast(MsgType.NOTIFICA, rn);

            } else if (type == FailureType.BOTH) {
                if (rn.nextInt(2) == 0)
                    randBcast(MsgType.NOTIFICA, rn);

            } else {
                // operate usually
                bcast(MsgType.NOTIFICA, v);
            }

            await(MsgType.NOTIFICA);

            /* PROPOSAL PHASE */
            MajTally proposalRes;
            synchronized (msgs) {
                printBufferSize(MsgType.NOTIFICA);
                proposalRes = getMajTally(msgs, MsgType.NOTIFICA, round);
            }

            if (type == FailureType.OMISSION) {

            } else if (type == FailureType.RANDOM_SIMPLE) {
                bcast(MsgType.PROPOSAL, rn.nextInt(2));

            } else if (type == FailureType.RANDOM_COMPLEX) {
                randBcast(MsgType.PROPOSAL, rn);

            } else if (type == FailureType.BOTH) {
                if (rn.nextInt(2) == 0)
                    randBcast(MsgType.NOTIFICA, rn);

            } else {
                // operate usually
                if (proposalRes.tally > (n + f) / 2) {
                    bcast(MsgType.PROPOSAL, proposalRes.maj);
                } else {
                    bcast(MsgType.PROPOSAL, -1);
                }
            }

            if (decided)
                break;

            await(MsgType.PROPOSAL);

            /* DECISION PHASE */

            MajTally decisionRes;
            synchronized (msgs) {
                printBufferSize(MsgType.PROPOSAL);
                decisionRes = getMajTally(msgs, MsgType.PROPOSAL, round);
            }

            if (decisionRes.tally > f) {
                v = decisionRes.maj;
                if (decisionRes.tally > 3*f) {
                    decide();
                    decided = true;
                }
            } else {
                v = rn.nextInt(2);
            }

            prepareNewRound();
            round++;
        }
    }

    private void await(MsgType type) throws InterruptedException {
        while (true) {
            synchronized (msgs) {
                if (msgs.stream().filter(p -> p.t == type && p.r == round).count() >= n - f)
                    break;
            }
            Thread.sleep(100);
        }
    }

    private void printBufferSize(MsgType type) {
        long cnt = msgs.stream().filter(p -> p.t == type && p.r == round).count();
        System.out.printf("%s%d -> (Recv %s, r: %4d, n: %4d)%s\n", ANSI_GREEN, id, type.toString(), round, cnt, ANSI_RESET);
    }

    public void handleMsg(MsgType type, int r, int w) throws RemoteException {
        synchronized (msgs) {
            msgs.add(new Triple<>(type, r, w));
        }
    }

    private void bcast(MsgType type, int w) throws RemoteException, MalformedURLException {
        System.out.printf("%d -> (Send %s, r: %4d, w: %4d)\n", id, type.toString(), round, w);
        for (String addr : addrs) {
            try {
                send(type, addr, round, w);
            } catch (NotBoundException e) {
                System.err.printf("Sending failed on %s\n", addr);
                e.printStackTrace();
            }
        }
    }

    private void randBcast(MsgType type, Random rn) throws RemoteException, MalformedURLException {
        System.out.printf("%d -> (Send %s, r: %4d, w: random)\n", id, type.toString(), round);
        for (String addr : addrs) {
            try {
                send(type, addr, round, rn.nextInt(2));
            } catch (NotBoundException e) {
                System.err.printf("Sending failed on %s\n", addr);
                e.printStackTrace();
            }
        }
    }

    private static void send(MsgType type, String dest, int r, int w)
            throws RemoteException, MalformedURLException, NotBoundException {
        Byzantine_RMI remote = (Byzantine_RMI) Naming.lookup(dest);
        remote.handleMsg(type, r, w);
    }

    private void prepareNewRound() {
        synchronized (msgs) {
            msgs = msgs.stream().filter(p -> !p.r.equals(round)).collect(Collectors.toList());
        }
    }

    private void decide() {
        System.out.printf("%s>>> Node %d DECIDED on %d in round %d <<<%s\n", ANSI_RED, id, v, round, ANSI_RESET);
    }

    private static MajTally getMajTally(List<Triple<MsgType, Integer, Integer>> triple, MsgType type, Integer r) {
        long tally0 = getTallyOf(triple, type, 0, r);
        long tally1 = getTallyOf(triple, type, 1, r);

        int maj = 0;
        long tally = tally0;
        if (tally0 < tally1) {
            maj = 1;
            tally = tally1;
        }

        return new MajTally(maj, tally);
    }

    private static long getTallyOf(List<Triple<MsgType, Integer, Integer>> triple, MsgType type, Integer w, Integer r) {
        return triple.stream().filter(p -> p.t == type && p.w == w && p.r == r).count();
    }
}
