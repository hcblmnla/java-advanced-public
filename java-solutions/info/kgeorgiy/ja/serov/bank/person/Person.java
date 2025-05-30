package info.kgeorgiy.ja.serov.bank.person;

import info.kgeorgiy.ja.serov.bank.model.Bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Person of the {@link Bank bank} API.
 */
public interface Person extends Remote {

    /** Returns person's firstname. */
    String firstName() throws RemoteException;

    /** Returns person's lastname. */
    String lastName() throws RemoteException;

    /** Returns person's passport number. */
    String passportNumber() throws RemoteException;
}
