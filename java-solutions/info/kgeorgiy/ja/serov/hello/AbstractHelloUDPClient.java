package info.kgeorgiy.ja.serov.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloClient;

import java.net.SocketAddress;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Abstract UDP implementation of {@link NewHelloClient new hello client}.
 *
 * @author alnmlbch
 */
public abstract class AbstractHelloUDPClient
    implements NewHelloClient, BiPredicate<String, String> {

    /** Specified timeout in milliseconds. */
    protected static final int TIMEOUT_IN_MILLIS = 50;

    /** Sockets map due to {@code O(1)} using. */
    protected final Map<Request, SocketAddress> addresses;

    /** Default constructor using sockets map. */
    protected AbstractHelloUDPClient(final Map<Request, SocketAddress> addresses) {
        this.addresses = addresses;
    }

    /**
     * New Hello UDP client launcher.
     * <p>
     * Usage: {@code clientName <host> <port> <prefix> <requests> <threads>}.
     * Where from the non-obvious {@code requests} is the number of requests in each thread.
     *
     * @param instance client instance
     * @param args     command line arguments
     */
    public static void invoke(final NewHelloClient instance, final String... args) {
        if (args.length < 5) {
            System.err.println("""
                Usage: HelloUDPClient <host> <port> <prefix> <requests> <threads>
                """);
            return;
        }
        final String host = args[0];
        final String prefix = args[2];
        try {
            final int port = Integer.parseInt(args[1]);
            final int requests = Integer.parseInt(args[3]);
            final int threads = Integer.parseInt(args[4]);

            instance.run(host, port, prefix, requests, threads);
        } catch (final NumberFormatException _) {
            System.err.println("""
                Arguments like port, requests and threads should be a positive integer
                """);
        }
    }

    @Override
    public boolean test(final String response, final String message) {
        return response.contains(message);
    }
}
