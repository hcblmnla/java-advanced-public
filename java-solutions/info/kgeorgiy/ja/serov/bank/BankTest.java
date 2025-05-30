package info.kgeorgiy.ja.serov.bank;

import info.kgeorgiy.ja.serov.bank.account.Account;
import info.kgeorgiy.ja.serov.bank.model.Bank;
import info.kgeorgiy.ja.serov.bank.model.RemoteBank;
import info.kgeorgiy.ja.serov.bank.person.LocalPerson;
import info.kgeorgiy.ja.serov.bank.person.Person;
import info.kgeorgiy.ja.serov.bank.person.RemotePerson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BankTest {

    public static void main(final String[] args) {
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(BankTest.class))
            .build();

        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        final Launcher launcher = LauncherFactory.create();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        final TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        System.exit(Boolean.compare(true, summary.getTotalFailureCount() == 0));
    }

    @BeforeAll
    static void startRmi() throws Exception {
        Server.startServer(7010);
    }

    static void startRmiAndExportBank(final int rmiPort, final int bankPort) throws Exception {
        Server.startServer(rmiPort);
        final Bank initial = new RemoteBank(bankPort);
        UnicastRemoteObject.exportObject(initial, bankPort);
        Naming.rebind("//localhost:%d/bank".formatted(rmiPort), initial);
    }

    <P extends Person> void personsTypesTest(
        final int port,
        final ExecutorService executor,
        final BiFunctionE<Bank, String, List<P>> personsF
    ) throws Exception {
        indexedBankTest(
            port,
            executor,
            (bank, i) -> bank.createPerson("f_name" + i, "l_name" + i, "P" + (i % 17)),
            bank -> personsF.apply(bank, "P" + 15),
            Person::firstName,
            6
        );
    }

    @Test
    void getPersonsByPassportNumber_shouldReturnAllPossiblePersons() throws Exception {
        personsTypesTest(
            7031,
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
            Bank::getPersonsByPassportNumber
        );
    }

    @Test
    void getLocalPersonsByPassportNumber_shouldReturnAllPossiblePersons() throws Exception {
        personsTypesTest(
            7032,
            Executors.newCachedThreadPool(),
            Bank::getLocalPersonsByPassportNumber
        );
    }

    @Test
    void getAccountsByPerson_shouldReturnAllPossibleAccounts() throws Exception {
        indexedBankTest(
            7033,
            Executors.newVirtualThreadPerTaskExecutor(),
            (bank, i) -> bank.createAccount("pass%d:acc%d".formatted(i % 17, i)),
            bank -> bank.getAccountsByPerson(new RemotePerson("TestF", "TestL", "pass15")),
            account -> account.getId().split(":")[1],
            3
        );
    }

    <T> void indexedBankTest(
        final int port,
        final ExecutorService executor,
        final ObjIntConsumerE<Bank> bankAction,
        final FunctionE<Bank, List<T>> listInjector,
        final FunctionE<T, String> stringInjector,
        final int beginIndex
    ) throws Exception {
        final Bank bank = new RemoteBank(port);
        IntStream.range(0, 100)
            .mapToObj(i -> CompletableFuture.runAsync(
                () -> {
                    try {
                        bankAction.accept(bank, i);
                    } catch (final Exception e) {
                        throw new AssertionError(e);
                    }
                },
                executor
            ))
            .forEach(CompletableFuture::join);

        final List<T> values = listInjector.apply(bank);
        assertEquals(100 / 17, values.size());

        values.forEach(value -> {
            try {
                final String s = stringInjector.apply(value).substring(beginIndex);
                assertEquals(15, Integer.parseInt(s) % 17);
            } catch (final Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    @FunctionalInterface
    interface ObjIntConsumerE<T> {
        void accept(T value, int i) throws Exception;
    }

    @FunctionalInterface
    interface FunctionE<T, R> {
        R apply(T value) throws Exception;
    }

    @FunctionalInterface
    interface BiFunctionE<T, U, R> {
        R apply(T t, U u) throws Exception;
    }

    @Nested
    class ClientTest {
        ByteArrayOutputStream out;
        ByteArrayOutputStream err;

        @BeforeAll
        static void startRmi() throws Exception {
            startRmiAndExportBank(1099, 4002);
        }

        @BeforeEach
        void updateOut() {
            out = new ByteArrayOutputStream();
            err = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));
            System.setErr(new PrintStream(err));
        }

        @Test
        void main_shouldPrintUpdatedAmount() {
            final String firstname = "Daniil 京都市";
            final String lastname = "\uD83E\uDD12 Serov";

            Client.main(firstname, lastname, "409549", "abc", "500");
            assertTrue(out.toString().contains("500"));

            Client.main(firstname, lastname, "409549", "abc", "200");
            assertTrue(out.toString().contains("700"));
        }

        @Test
        void main__shouldPrintUsage_ifInvalidArgumentsNumber() {
            Client.main("Daniil", "Serov", "409549", "abc_500");
            assertTrue(err.toString().contains("Usage"));
        }

        @Test
        void main__shouldPrintSomething_ifInvalidArguments() {
            Client.main("Daniil", "Serov", "409549", "abc", "non-integer500");
            assertFalse(err.toString().isBlank());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SharedBankTest {

        Bank bank;

        @BeforeAll
        static void startRmi() throws Exception {
            startRmiAndExportBank(7011, 7021);
        }

        @BeforeEach
        void updateBank() throws Exception {
            bank = (Bank) Naming.lookup("//localhost:%d/bank".formatted(7011));
        }

        @Test
        @Order(1)
        void createAccount_shouldReturnNewAccountWithZeroBalance() throws Exception {
            final Account account = bank.createAccount("shared");
            assertEquals(0, account.getAmount());

            account.setAmount(100);
            assertEquals(100, account.getAmount());

            final Account copy = bank.createAccount("shared");
            assertEquals(100, copy.getAmount());
        }

        @Test
        @Order(2)
        void getAccount_shouldReturnBalancedAccount() throws Exception {
            final Account account = bank.createAccount("shared");
            account.increaseAmount(150);

            assertEquals(250, bank.getAccount("shared").getAmount());

            final Account another = bank.createAccount("shared2");
            assertEquals(0, another.getAmount());

            another.increaseAmount(-100);
            assertEquals(-100, bank.getAccount("shared2").getAmount());
        }
    }

    @Nested
    class LocalPersonTest {

        @Test
        void emptyLocalPerson_shouldSerializeAndDeserialize() throws Exception {
            shouldSerializeAndDeserialize(_ -> {
                // empty
            });
        }

        @Test
        void filledLocalPerson_shouldSerializeAndDeserialize() throws Exception {
            shouldSerializeAndDeserialize(
                person -> IntStream
                    .range(0, 100)
                    .forEach(acc -> {
                        try {
                            person.createAccount(Integer.toHexString(acc));
                        } catch (final RemoteException e) {
                            throw new AssertionError(e);
                        }
                    })
            );
        }

        void shouldSerializeAndDeserialize(final Consumer<LocalPerson> action) throws Exception {
            final LocalPerson original = new LocalPerson("Serov", "Daniil", "409549");
            action.accept(original);
            final byte[] bytes;
            try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)
            ) {
                oos.writeObject(original);
                bytes = bos.toByteArray();
            }
            final LocalPerson copy;
            try (
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bis)
            ) {
                copy = (LocalPerson) ois.readObject();
            }
            assertEquals(original, copy);
        }
    }
}
