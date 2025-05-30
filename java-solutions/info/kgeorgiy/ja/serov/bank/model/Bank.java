package info.kgeorgiy.ja.serov.bank.model;

import info.kgeorgiy.ja.serov.bank.account.Account;
import info.kgeorgiy.ja.serov.bank.manager.AccountManager;
import info.kgeorgiy.ja.serov.bank.person.LocalPerson;
import info.kgeorgiy.ja.serov.bank.person.Person;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Fully bank API. Extends {@link AccountManager} API.
 */
public interface Bank extends AccountManager {

    /** Returns all {@link Person persons} with specified passport number. */
    List<Person> getPersonsByPassportNumber(String passportNumber) throws RemoteException;

    /** Returns all {@link LocalPerson local persons} with specified passport number. */
    List<LocalPerson> getLocalPersonsByPassportNumber(String passportNumber) throws RemoteException;

    /**
     * Tries to create new {@link Person person}.
     *
     * @param firstName      person's firstname
     * @param lastName       person's lastname
     * @param passportNumber person's passport number
     * @return {@code true} if person was not in bank else {@code false}
     * @throws RemoteException if error occurred
     */
    boolean createPerson(
        String firstName,
        String lastName,
        String passportNumber
    ) throws RemoteException;

    /** Returns all {@link Account accounts} of specified {@link Person person}. */
    List<Account> getAccountsByPerson(Person person) throws RemoteException;
}
