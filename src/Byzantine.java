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
    private List<Tuple<Integer, Integer>> msgsN;
    private List<Tuple<Integer, Integer>> msgsP;

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

    static class Tuple<X, Y> {
        public final X r;
        public final Y w;
        public Tuple(X r, Y w) {
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

    public Byzantine(int id, int n, int f, int v, List<String> _addrs, FailureType type) throws RemoteException {
        round = 0;
        decided = false;
        this.id = id;
        this.n = n;
        this.f = f;
        this.v = v;
        addrs = new ArrayList<>(_addrs);
        msgsN = new ArrayList<>();
        msgsP = new ArrayList<>();
        this.type = type;

        assert addrs.size() == n;
        System.out.printf("%d initiated with v = %d, type = %s\n", id, v, type.toString());
    }

    public void run()
            throws RemoteException, MalformedURLException, InterruptedException {
        Random rn = new Random();
        for (;;) {
            // artificial delay up to 100 ms
            Thread.sleep(rn.nextInt(100));

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

            // await n - f messages in the form of (N, r, *)
            while (true) {
                synchronized (msgsN) {
                    if (msgsN.stream().filter(p -> p.r == round).count() >= n - f)
                        break;
                }
                Thread.sleep(100);
            }

            /* PROPOSAL PHASE */
            MajTally proposalRes;
            synchronized (msgsN) {
                printBufferSize(MsgType.NOTIFICA, msgsN);
                proposalRes = getMajTally(msgsN, round);
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

            // await n - f messages in the form of (P, r, *)
            while (true) {
                synchronized (msgsP) {
                    if (msgsP.stream().filter(p -> p.r == round).count() >= n - f)
                        break;
                }
                Thread.sleep(100);
            }

            /* DECISION PHASE */

            MajTally decisionRes;
            synchronized (msgsP) {
                printBufferSize(MsgType.PROPOSAL, msgsP);
                decisionRes = getMajTally(msgsP, round);
            }

            if (decisionRes.tally > f) {
                v = decisionRes.maj;
                if (decisionRes.tally > 3*f) {
                    decide(v);
                    decided = true;
                }
            } else {
                v = rn.nextInt(2);
            }

            prepareNewRound();
            round++;
        }
    }

    private void printBufferSize(MsgType type, List<Tuple<Integer, Integer>> msgs) {
        long cnt = msgs.stream().filter(p -> p.r == round).count();
        System.out.printf("%s%d -> (Recv %s, r: %4d, n: %4d)%s\n", ANSI_GREEN, id, type.toString(), round, cnt, ANSI_RESET);
    }

    public void handleMsg(MsgType type, int r, int w) throws RemoteException {
        if (type == MsgType.NOTIFICA) {
            synchronized (msgsN) {
                msgsN.add(new Tuple<>(r, w));
            }
        } else if (type == MsgType.PROPOSAL) {
            synchronized (msgsP) {
                msgsP.add(new Tuple<>(r, w));
            }
        } else {
            throw new RemoteException("Invalid Msg Type!");
        }
    }

    private void bcast(MsgType type, int w)
            throws RemoteException, MalformedURLException {
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

    private void randBcast(MsgType type, Random rn)
            throws RemoteException, MalformedURLException {
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

    private synchronized void prepareNewRound() {
        msgsN = filterMsgList(msgsN, round);
        msgsP = filterMsgList(msgsP, round);
    }

    private void decide(int w) {
        System.out.printf("%s>>> Node %d DECIDED on %d in round %d<<<%s\n", ANSI_RED, id, w, round, ANSI_RESET);
    }

    private static List<Tuple<Integer, Integer>> filterMsgList(List<Tuple<Integer, Integer>> msgs, Integer r) {
        return msgs.stream().filter(p -> p.r != r).collect(Collectors.toList());
    }

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
