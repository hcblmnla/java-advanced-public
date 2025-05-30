package info.kgeorgiy.ja.serov.bank.person;

/**
 * Remote {@link Person} implementation.
 *
 * @param firstName      person's firstname
 * @param lastName       person's lastname
 * @param passportNumber person's passport number
 */
public record RemotePerson(String firstName, String lastName, String passportNumber)
    implements Person {
}
