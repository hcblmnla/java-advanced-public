package info.kgeorgiy.ja.serov.bank.account;

import info.kgeorgiy.ja.serov.bank.model.RemoteBank;
import info.kgeorgiy.ja.serov.bank.person.LocalPerson;

import java.io.Serial;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Bank account API.
 * <p>
 * Inherits {@link Remote} due to {@link RemoteBank} using;
 * inherits {@link Serializable} due to {@link LocalPerson} using.
 */
public interface Account extends Remote, Serializable {

    /** Serial version UID. */
    @Serial
    long serialVersionUID = 654654654L;

    /** Returns account identifier. */
    String getId() throws RemoteException;

    /** Returns amount of money in the account. */
    int getAmount() throws RemoteException;

    /** Sets amount of money in the account. */
    void setAmount(int amount) throws RemoteException;

    /** Increases amount of money in the account. */
    void increaseAmount(int amount) throws RemoteException;
}
