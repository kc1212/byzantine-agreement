import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Byzantine_Main {

    public static List<String> makeAddrs(int n) {
        List<String> addrs = new ArrayList<>();
        final String RMI_PREFIX = "rmi://localhost:1099/byz-";
        for (int i = 0; i < n; i++) {
            addrs.add(RMI_PREFIX + i);
        }
        return addrs;
    }

    public static void main(String args[]) {
        // java Byzantine_Main.class <n> <f>
        if (args.length == 2) {
            int n = Integer.parseInt(args[0]);
            int f = Integer.parseInt(args[1]);
            List<String> addrs = makeAddrs(n);

            for (int i = 0; i < n; i++) {
                final int I = i;
                final int N = n;
                final int F = f;
                final List<String> ADDRS = addrs;
                Runnable task = () -> {
                    try {
                        int v = (new Random()).nextInt() % 2;
                        Byzantine byz = new Byzantine(I, N, F, v, ADDRS);
                        Naming.rebind(ADDRS.get(I), byz);
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
            System.err.println("usage: java Byzantine_Main <n> <f>");
        }

    }
}
