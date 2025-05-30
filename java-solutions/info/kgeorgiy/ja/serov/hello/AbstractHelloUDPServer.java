package info.kgeorgiy.ja.serov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Abstract UDP implementation of {@link HelloServer hello server}.
 *
 * @param <C> context implementation
 * @author alnmlbch
 */
public abstract class AbstractHelloUDPServer<C extends Closeable> implements HelloServer {

    /** Contexts map. */
    protected final ConcurrentMap<Integer, C> contexts;

    /** Running flag. */
    protected final AtomicBoolean running;

    /** Default constructor using contexts map. */
    protected AbstractHelloUDPServer(final ConcurrentMap<Integer, C> contexts) {
        this.contexts = contexts;
        this.running = new AtomicBoolean();
    }

    /**
     * Hello UDP server launcher.
     * <p>
     * Usage: {@code serverName <port> <threads>}.
     *
     * @param instance server instance constructor
     * @param args     command line arguments
     */
    public static void invoke(final Supplier<HelloServer> instance, final String... args) {
        if (args.length < 2) {
            System.err.println("Usage: HelloUDPServer <port> <threads>");
            return;
        }
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            try (HelloServer server = instance.get()) {
                server.start(port, threads);
            }
        } catch (final NumberFormatException _) {
            System.err.println("All arguments should be a positive integer");
        }
    }

    /**
     * Runs server after running checking.
     *
     * @param port    server port
     * @param threads number of working threads
     */
    protected abstract void startSafely(int port, int threads);

    @Override
    public void start(final int port, final int threads) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        startSafely(port, threads);
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            contexts.values().forEach(c -> {
                try {
                    c.close();
                } catch (final IOException e) {
                    System.err.format(
                        "Error closing %s due to %s%n",
                        c.getClass().getSimpleName(),
                        e.getMessage()
                    );
                }
            });
            contexts.clear();
        }
    }
}
