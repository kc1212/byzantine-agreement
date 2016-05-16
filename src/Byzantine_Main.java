import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Random;

public class Byzantine_Main {

    final static int PORT_NUMBER = 1099;

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

    static void startByzantine(int n, int f, int i, Byzantine.FailureType type, int port) {
        try {
            int v = (new Random()).nextInt(2);
            Byzantine byz = new Byzantine(i, n, f, v, type, port);
            byz.run();
        } catch (RemoteException | MalformedURLException | InterruptedException | AlreadyBoundException e) {
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
            int i = Integer.parseInt(args[2]);

            Byzantine.FailureType type;
            if (args.length >= 4)
                type = parseFailureType(args[3]);
            else
                type = Byzantine.FailureType.NOFAILURE;

            startByzantine(n, f, i, type, PORT_NUMBER);

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

            Byzantine.FailureType type;
            if (args.length >= 3)
                type = parseFailureType(args[2]);
            else
                type = Byzantine.FailureType.NOFAILURE;

            Thread threads[] = new Thread[n];
            for (int i = 0; i < n; i++) {
                final int I = i;
                final Byzantine.FailureType Type = i < f ? type : Byzantine.FailureType.NOFAILURE;
                Runnable task = () -> startByzantine(n, f, I, Type, PORT_NUMBER);
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
