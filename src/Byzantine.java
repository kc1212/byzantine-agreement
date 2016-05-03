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
        RANDOM,
        BOTH
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
            // artificial delay up to 1 second
            Thread.sleep(rn.nextInt(1000));

            /* NOTIFICA PHASE */

            if (type == FailureType.OMISSION) {
                // do nothing

            } else if (type == FailureType.RANDOM) {
                // broadcast random value
                bcast(MsgType.NOTIFICA, round, rn.nextInt(2));

            } else if (type == FailureType.BOTH) {
                // broadcast random value half of the time
                if (rn.nextInt(2) == 0)
                    bcast(MsgType.NOTIFICA, round, rn.nextInt(2));

            } else {
                // operate usually
                bcast(MsgType.NOTIFICA, round, v);
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

            MajTally proposalRes = getMajTally(msgsN, round);

            if (type == FailureType.OMISSION) {
                // do nothing

            } else if (type == FailureType.RANDOM) {
                // broadcast random value
                bcast(MsgType.PROPOSAL, round, rn.nextInt(3) - 1);

            } else if (type == FailureType.BOTH) {
                // broadcast random value half of the time
                if (rn.nextInt(2) == 0)
                    bcast(MsgType.PROPOSAL, round, rn.nextInt(3) - 1);

            } else {
                // operate usually
                if (proposalRes.tally > (n + f) / 2) {
                    bcast(MsgType.PROPOSAL, round, proposalRes.maj);
                } else {
                    bcast(MsgType.PROPOSAL, round, -1);
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

            MajTally decisionRes = getMajTally(msgsP, round);

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

    private void bcast(MsgType type, int r, int w)
            throws RemoteException, MalformedURLException {
        System.out.printf("%d -> (%s, r: %d, w: %2d)\n", id, type.toString(), r, w);
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
        remote.handleMsg(type, r, w);
    }

    private synchronized void prepareNewRound() {
        msgsN = filterMsgList(msgsN, round);
        msgsP = filterMsgList(msgsP, round);
    }

    private void decide(int w) {
        System.out.printf(">>> Node %d DECIDED on %d <<<\n", id, w);
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
