package info.kgeorgiy.ja.serov.bank.person;

import info.kgeorgiy.ja.serov.bank.account.Account;
import info.kgeorgiy.ja.serov.bank.manager.AbstractAccountManager;

import java.io.Serial;
import java.util.concurrent.ConcurrentHashMap;

/** Local (serializable) {@link Person} implementation. */
public class LocalPerson extends AbstractAccountManager implements Person {

    @Serial
    private static final long serialVersionUID = 5850420585L;

    private final String firstName;
    private final String lastName;
    private final String passportNumber;

    /** Creates new person with specified data. */
    public LocalPerson(
        final String firstName,
        final String lastName,
        final String passportNumber
    ) {
        super(new ConcurrentHashMap<>());
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportNumber = passportNumber;
    }

    @Override
    public String firstName() {
        return firstName;
    }

    @Override
    public String lastName() {
        return lastName;
    }

    @Override
    public String passportNumber() {
        return passportNumber;
    }

    @Override
    public void exportAccount(final Account account) {
        // do nothing because local
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof final LocalPerson person) {
            return firstName.equals(person.firstName)
                && lastName.equals(person.lastName)
                && passportNumber.equals(person.passportNumber)
                && accounts.equals(person.accounts);
        }
        return false;
    }
}
