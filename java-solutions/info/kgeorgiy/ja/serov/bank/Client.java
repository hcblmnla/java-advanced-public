package info.kgeorgiy.ja.serov.bank;

import info.kgeorgiy.ja.serov.bank.account.Account;
import info.kgeorgiy.ja.serov.bank.model.Bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * {@link Bank} simple client.
 */
public enum Client {
    ;

    private final static int RMI_PORT = 1099;

    /**
     * Client launcher.
     * <p>
     * Usage: Client [firstName] [lastName] [passportNumber] [accountId] [amount]
     *
     * @param args command line arguments
     */
    public static void main(final String... args) {
        try {
            final Bank bank = (Bank) Naming.lookup("//localhost:%d/bank".formatted(RMI_PORT));
            mainE(bank, args);
        } catch (final RemoteException e) {
            System.err.println("Internal (remote) error: " + e.getMessage());
            System.exit(1);
        } catch (final NotBoundException e) {
            System.err.println("Bank is not bound");
        } catch (final MalformedURLException e) {
            System.err.println("Bank URL is invalid");
        }
    }

    private static void mainE(final Bank bank, final String... args) throws RemoteException {
        if (args.length < 5) {
            System.err.println("""
                Usage: Client [firstName] [lastName] [passportNumber] [accountId] [amount]
                """);
            return;
        }

        final String firstName = args[0];
        final String lastName = args[1];
        final String passportNumber = args[2];
        final String accountId = args[3];

        final int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (final NumberFormatException e) {
            System.err.println("Non-integer amount for account " + accountId);
            return;
        }

        System.out.format("%s person: %s %s%n".formatted(
            bank.createPerson(firstName, lastName, passportNumber)
                ? "Created"
                : "Got",
            firstName,
            lastName
        ));

        final Account account = bank.createAccount(passportNumber + ":" + accountId);
        account.increaseAmount(amount);
        System.out.format(
            "New amount %d for account %s of %s %s%n",
            account.getAmount(),
            accountId,
            firstName,
            lastName
        );
    }
}
