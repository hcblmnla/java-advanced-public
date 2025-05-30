package info.kgeorgiy.ja.serov.bank.model;

import info.kgeorgiy.ja.serov.bank.account.Account;
import info.kgeorgiy.ja.serov.bank.manager.AbstractAccountManager;
import info.kgeorgiy.ja.serov.bank.person.LocalPerson;
import info.kgeorgiy.ja.serov.bank.person.Person;
import info.kgeorgiy.ja.serov.bank.person.RemotePerson;

import java.io.UncheckedIOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Remote {@link Bank bank} implementation.
 */
public class RemoteBank extends AbstractAccountManager implements Bank {

    private final ConcurrentMap<String, Set<Person>> persons;
    private final int port;

    /**
     * Default remote constructor which using specified port.
     *
     * @param port port
     */
    public RemoteBank(final int port) {
        super(new ConcurrentHashMap<>());
        this.persons = new ConcurrentHashMap<>();
        this.port = port;
    }

    @Override
    public void exportAccount(final Account account) throws RemoteException {
        export(account);
    }

    @Override
    public List<Person> getPersonsByPassportNumber(final String passportNumber) {
        System.out.println("Retrieving persons by passport number " + passportNumber);
        return persons.get(passportNumber).stream().toList();
    }

    @Override
    public List<LocalPerson> getLocalPersonsByPassportNumber(final String passportNumber) {
        System.out.println("Retrieving local persons");
        return getPersonsByPassportNumber(passportNumber)
            .stream()
            .map(person -> {
                try {
                    return new LocalPerson(person.firstName(), person.lastName(), passportNumber);
                } catch (final RemoteException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public boolean createPerson(
        final String firstName,
        final String lastName,
        final String passportNumber
    ) throws RemoteException {
        System.out.println("Creating person " + firstName + " " + lastName);
        final Person person = new RemotePerson(firstName, lastName, passportNumber);
        final Set<Person> passportPersons = persons.computeIfAbsent(
            passportNumber,
            _ -> new HashSet<>()
        );
        if (!passportPersons.add(person)) {
            export(person);
            return true;
        }
        return false;
    }

    @Override
    public List<Account> getAccountsByPerson(final Person person) {
        System.out.println("Retrieving accounts by person " + person);
        return accounts.entrySet()
            .stream()
            .filter(acc -> {
                final String id = acc.getKey();
                try {
                    final String prefix = person.passportNumber() + ":";
                    return id.startsWith(prefix) && id.length() > prefix.length();
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .map(Map.Entry::getValue)
            .toList();
    }

    private void export(final Remote remote) throws RemoteException {
        UnicastRemoteObject.exportObject(remote, port);
    }
}
