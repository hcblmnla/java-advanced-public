package info.kgeorgiy.ja.serov.bank.manager;

import info.kgeorgiy.ja.serov.bank.account.Account;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Basic {@link Account accounts} operations API.
 */
public interface AccountManager extends Remote {

    /**
     * Creates a new account with specified identifier if it does not already exist.
     *
     * @param id account id
     * @return created or existing account
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Returns account by identifier.
     *
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist
     */
    Account getAccount(String id) throws RemoteException;
}
