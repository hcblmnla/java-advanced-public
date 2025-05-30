package info.kgeorgiy.ja.serov.bank;

import info.kgeorgiy.ja.serov.bank.model.Bank;
import info.kgeorgiy.ja.serov.bank.model.RemoteBank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * {@link Bank} simple server.
 */
public enum Server {
    ;

    private final static int RMI_PORT = 1099;
    private final static int BANK_DEFAULT_PORT = 4002;

    /** Server launcher. */
    public static void main(final String... args) {
        final int bankPort = args.length > 0
            ? Integer.parseInt(args[0])
            : BANK_DEFAULT_PORT;

        final Bank bank = new RemoteBank(bankPort);
        try {
            startServer(RMI_PORT);
            UnicastRemoteObject.exportObject(bank, bankPort);

            Naming.rebind("//localhost:%d/bank".formatted(RMI_PORT), bank);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.err.println("Cannot export object: " + e.getMessage());
            System.exit(1);
        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL");
        }
    }

    /**
     * Safely starter {@code rmiregistry} with given port.
     *
     * @param port port
     * @throws RemoteException if error occurred
     */
    public static void startServer(final int port) throws RemoteException {
        try {
            final Registry registry = LocateRegistry.getRegistry(port);
            registry.list();
        } catch (final RemoteException e) {
            LocateRegistry.createRegistry(port);
        }
    }
}
