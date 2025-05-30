package info.kgeorgiy.ja.serov.bank.manager;

import info.kgeorgiy.ja.serov.bank.account.Account;
import info.kgeorgiy.ja.serov.bank.account.RemoteAccount;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

/** Abstract {@link AccountManager account manager} implementation. */
public abstract class AbstractAccountManager implements AccountManager, Serializable {

    /** Thread-safe accounts storage. */
    protected final ConcurrentMap<String, Account> accounts;

    /** Creates new instance with given storage. */
    protected AbstractAccountManager(final ConcurrentMap<String, Account> accounts) {
        this.accounts = accounts;
    }

    /**
     * {@link Account} exporter to for possible implementations.
     *
     * @param account given account
     * @throws RemoteException if error occurred
     */
    protected abstract void exportAccount(Account account) throws RemoteException;

    @Override
    public Account createAccount(final String id) throws RemoteException {
        System.out.println("Creating account " + id);
        try {
            return accounts.computeIfAbsent(id, i -> {
                final Account account = new RemoteAccount(i);
                try {
                    exportAccount(account);
                } catch (final RemoteException e) {
                    throw new RuntimeException(e);
                }
                return account;
            });
        } catch (final RuntimeException e) {
            if (e.getCause() instanceof final RemoteException remoteE) {
                throw remoteE;
            }
            throw e;
        }
    }

    @Override
    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }
}
