import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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
        String str = "";
        for (Byzantine.FailureType t : Byzantine.FailureType.values()) {
            str += t.toString() + "|";
        }
        str = str.substring(0, str.length() - 1); // remove the last "|" character

        System.err.println("Invalid argument!");
        System.err.println("usage:\tjava Byzantine_Main single <n> <f> <id> [<" + str + ">]\n" +
                              "or:\tjava Byzantine_Main multi  <n> <f>      [<" + str + ">]");
    }

    static void startByzantine(int n, int f, List<String> addrs, int i, Byzantine.FailureType type) {
        try {
            int v = (new Random()).nextInt(2);
            Byzantine byz = new Byzantine(i, n, f, v, addrs, type);
            Naming.rebind(addrs.get(i), byz);
            Thread.sleep(1000); // wait for other nodes to come online
            byz.run();
            Thread.sleep(1000); // wait for all other nodes to finish the final round
            try {
                Naming.unbind(addrs.get(i));
                UnicastRemoteObject.unexportObject(byz, false);
            } catch (NotBoundException e) {
                // this shouldn't happen
                System.err.println("Failure!");
                e.printStackTrace();
            }
        } catch (RemoteException | MalformedURLException | InterruptedException e) {
            System.err.println("Failure!");
            e.printStackTrace();
        }
    }

    static Byzantine.FailureType parseFailureType(String str) {
        Byzantine.FailureType type = Byzantine.FailureType.NOFAILURE;
        for (Byzantine.FailureType t : Byzantine.FailureType.values()) {
            if (str.compareToIgnoreCase(t.toString()) == 0)
                type = t;
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

            Thread threads[] = new Thread[n];
            for (int i = 0; i < n; i++) {
                final int I = i;
                final Byzantine.FailureType Type = i < f ? type : Byzantine.FailureType.NOFAILURE;
                Runnable task = () -> startByzantine(n, f, addrs, I, Type);
                threads[i] = new Thread(task);
                threads[i].start();
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
