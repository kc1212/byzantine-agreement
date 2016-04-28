import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
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
        System.err.println("Invalid argument!");
        System.err.println("usage: java Byzantine_Main <n> <f> [<omission|random>]");
    }

    public static void main(String args[]) {
        // java Byzantine_Main.class <n> <f>
        if (args.length <= 3) {
            int n = Integer.parseInt(args[0]);
            int f = Integer.parseInt(args[1]);
            List<String> addrs = makeAddrs(n);

            Byzantine.FailureType type;
            try {
                if (args[2].compareToIgnoreCase(Byzantine.FailureType.OMISSION.toString()) == 0) {
                    type = Byzantine.FailureType.OMISSION;
                } else if (args[2].compareToIgnoreCase(Byzantine.FailureType.RANDOM.toString()) == 0) {
                    type = Byzantine.FailureType.RANDOM;
                } else {
                    usage();
                    return;
                }
            } catch (IndexOutOfBoundsException e) {
                type = Byzantine.FailureType.NOFAILURE;
            }

            for (int i = 0; i < n; i++) {
                final int I = i;
                final Byzantine.FailureType Type = i < f ? type : Byzantine.FailureType.NOFAILURE;
                Runnable task = () -> {
                    try {
                        int v = Math.abs((new Random()).nextInt() % 2);
                        Byzantine byz = new Byzantine(I, n, f, v, addrs, Type);
                        Naming.rebind(addrs.get(I), byz);
                        Thread.sleep(2); // wait for other nodes to come online
                        byz.run();
                    } catch (RemoteException | MalformedURLException | InterruptedException e) {
                        System.err.println("Failure!");
                        e.printStackTrace();
                    }
                };
                new Thread(task).start();
            }
        } else {
            System.err.println("Invalid argument!");
            System.err.println("usage: java Byzantine_Main <n> <f> [<omission|random>]");
            return;
        }
    }
}
