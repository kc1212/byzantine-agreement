import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Byzantine_Main {

    static List<String> makeAddrs(int n) {
        List<String> addrs = new ArrayList<>();
        final String RMI_PREFIX = "rmi://localhost:1099/byz-";
        for (int i = 0; i < n; i++) {
            addrs.add(RMI_PREFIX + i);
        }
        return addrs;
    }

    static void usage() {
        String str = Byzantine.FailureType.OMISSION.toString() + "|"
                + Byzantine.FailureType.RANDOM_SIMPLE.toString() + "|"
                + Byzantine.FailureType.RANDOM_COMPLEX.toString() + "|"
                + Byzantine.FailureType.BOTH.toString();
        System.err.println("Invalid argument!");
        System.err.println("usage:\tjava Byzantine_Main single <n> <f> <id> [<" + str + ">]\n" +
                              "or:\tjava Byzantine_Main multi  <n> <f>      [<" + str + ">]");
    }

    static void startByzantine(int n, int f, List<String> addrs, int i, Byzantine.FailureType type) {
        try {
            int v = (new Random()).nextInt(2);
            Byzantine byz = new Byzantine(i, n, f, v, addrs, type);
            Naming.rebind(addrs.get(i), byz);
            Thread.sleep(100); // wait for other nodes to come online
            byz.run();
        } catch (RemoteException | MalformedURLException | InterruptedException e) {
            System.err.println("Failure!");
            e.printStackTrace();
        }
    }

    static Byzantine.FailureType parseFailureType(String str) {
        Byzantine.FailureType type;
        if (str.compareToIgnoreCase(Byzantine.FailureType.OMISSION.toString()) == 0) {
            type = Byzantine.FailureType.OMISSION;
        } else if (str.compareToIgnoreCase(Byzantine.FailureType.RANDOM_SIMPLE.toString()) == 0) {
            type = Byzantine.FailureType.RANDOM_SIMPLE;
        } else if (str.compareToIgnoreCase(Byzantine.FailureType.RANDOM_COMPLEX.toString()) == 0) {
            type = Byzantine.FailureType.RANDOM_COMPLEX;
        } else if (str.compareToIgnoreCase(Byzantine.FailureType.BOTH.toString()) == 0) {
            type = Byzantine.FailureType.BOTH;
        } else {
            type = Byzantine.FailureType.NOFAILURE;
            System.out.println("Invalid failure type, defaulting to NOFAILURE.");
        }

        return type;
    }

    static void handleSingle(String args[]) {
        try {
            int n = Integer.parseInt(args[0]);
            int f = Integer.parseInt(args[1]);
            List<String> addrs = makeAddrs(n);
            int i = Integer.parseInt(args[2]);

            Byzantine.FailureType type;
            if (args.length >= 4)
                type = parseFailureType(args[3]);
            else
                type = Byzantine.FailureType.NOFAILURE;

            startByzantine(n, f, addrs, i, type);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            usage();
            return;
        }
    }

    static void handleMulti(String args[]) {
        try {
            int n = Integer.parseInt(args[0]);
            int f = Integer.parseInt(args[1]);
            List<String> addrs = makeAddrs(n);

            Byzantine.FailureType type;
            if (args.length >= 3)
                type = parseFailureType(args[2]);
            else
                type = Byzantine.FailureType.NOFAILURE;

            for (int i = 0; i < n; i++) {
                final int I = i;
                final Byzantine.FailureType Type = i < f ? type : Byzantine.FailureType.NOFAILURE;
                Runnable task = () -> startByzantine(n, f, addrs, I, Type);
                new Thread(task).start();
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
            usage();
            return;
        }
    }

    static String[] pop(String arr[]) {
        return Arrays.copyOfRange(arr, 1, arr.length);
    }

    public static void main(String args[]) {
        if (args.length < 3) {
            usage();
            return;
        }

        if (args[0].compareToIgnoreCase("multi") == 0) {
            handleMulti(pop(args));
        } else if (args[0].compareToIgnoreCase("single") == 0) {
            handleSingle(pop(args));
        } else {
            usage();
            return;
        }
    }
}
