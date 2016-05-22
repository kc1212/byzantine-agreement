import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Random;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Byzantine_Main {

    @Parameter(names = "-p", description = "the RMI port number to use")
    private Integer port_number = 1099;

    @Parameter(names = "-m", description = "the mode to use")
    private Mode mode = Mode.MULTI;

    @Parameter(names = "-n", required = true, description = "the total number of nodes")
    private Integer n;

    @Parameter(names = "-f", required = true, description = "the number of faulty nodes")
    private Integer f;

    @Parameter(names = "-i", description = "the id of the node (only in single mode)")
    private Integer id = null;

    @Parameter(names = "-t", description = "the failure type")
    private Byzantine.FailureType type = Byzantine.FailureType.NOFAILURE;

    @Parameter(names = "-h", help = true, description = "prints this message")
    private boolean help;

    enum Mode {
        SINGLE,
        MULTI
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

    void handleSingle() {
        if (id == null) {
            throw new ParameterException("The following options are required: -i is required for single mode");
        }
        startByzantine(n, f, id, type, port_number);
    }

    void handleMulti() {
        Thread threads[] = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int I = i;
            final Byzantine.FailureType Type = i < f ? type : Byzantine.FailureType.NOFAILURE;
            Runnable task = () -> startByzantine(n, f, I, Type, port_number);
            threads[i] = new Thread(task);
            threads[i].start();
        }
    }

    public void run() {
        if (mode == Mode.MULTI)
            handleMulti();
        else if (mode == Mode.SINGLE)
            handleSingle();

        throw new InternalError("Unhandled mode");
    }

    public static void main(String args[]) {
        Byzantine_Main main = new Byzantine_Main();

        JCommander jc = new JCommander(main, args);
        if (main.help) {
            jc.usage();
            return;
        }

        main.run();
    }
}
