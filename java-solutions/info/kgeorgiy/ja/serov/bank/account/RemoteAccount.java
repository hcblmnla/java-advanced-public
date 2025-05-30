package info.kgeorgiy.ja.serov.bank.account;

import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Remote {@link Account} implementation using {@code RMI}.
 */
public class RemoteAccount implements Account {

    private final String id;
    private final AtomicInteger amount;

    /**
     * Constructs new {@link Account} with zero balance.
     *
     * @param id account id
     */
    public RemoteAccount(final String id) {
        this.id = id;
        this.amount = new AtomicInteger();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount.get();
    }

    @Override
    public void setAmount(final int amount) {
        System.out.format("Setting amount %d of money for account %s%n", amount, id);
        this.amount.set(amount);
    }

    @Override
    public void increaseAmount(final int amount) throws RemoteException {
        System.out.format("Increasing amount %d of money for account %s%n", amount, id);
        this.amount.getAndAdd(amount);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof final RemoteAccount account) {
            return id.equals(account.id);
        }
        return false;
    }
}
