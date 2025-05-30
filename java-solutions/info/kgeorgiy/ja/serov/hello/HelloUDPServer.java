package info.kgeorgiy.ja.serov.hello;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Blocking UDP implementation of {@link AbstractHelloUDPServer abstract UDP server}.
 *
 * @author alnmlbch
 */
public class HelloUDPServer extends AbstractHelloUDPServer<HelloUDPServer.ConcurrentSocket> {

    /** Default constructor with no arguments. */
    public HelloUDPServer() {
        super(new ConcurrentHashMap<>());
    }

    /** {@link HelloUDPClient} launcher. */
    public static void main(final String... args) {
        invoke(HelloUDPServer::new, args);
    }

    @Override
    @SuppressWarnings("resource")
    public void startSafely(final int port, final int threads) {
        final DatagramSocket socket;
        final ExecutorService executor;
        final byte[] buffer;
        try {
            final ConcurrentSocket cs = socketOf(port, threads);
            socket = cs.socket();
            executor = cs.executor();
            buffer = cs.buffer();
        } catch (final SocketException e) {
            System.err.println("Socket server error occurred: " + e.getMessage());
            return;
        }
        final int offset = buffer.length / threads;
        IntStream
            .range(0, threads)
            .forEach(thread -> executor.submit(() -> {
                try {
                    while (running.get() && !socket.isClosed()) {
                        final DatagramPacket received = new DatagramPacket(
                            buffer, offset * thread, offset
                        );
                        socket.receive(received);
                        socket.send(HelloUtils.asPacket(
                            HelloUtils.asString(received),
                            received.getSocketAddress()
                        ));
                    }
                } catch (final IOException e) {
                    System.err.println("Request server error occurred: " + e.getMessage());
                }
            }));
    }

    private ConcurrentSocket socketOf(final int port, final int threads) throws SocketException {
        final ConcurrentSocket socket = contexts.get(port);
        if (socket != null) {
            return socket;
        }
        final ConcurrentSocket newSocket = new ConcurrentSocket(
            new DatagramSocket(port), Executors.newFixedThreadPool(threads)
        );
        contexts.put(port, newSocket);
        return newSocket;
    }

    /**
     * Helper parallel socker.
     *
     * @param socket   UDP socket
     * @param executor parallel executor
     * @param buffer   memory
     * @author alnmlbch
     */
    public record ConcurrentSocket(
        DatagramSocket socket,
        ExecutorService executor,
        byte[] buffer
    ) implements Closeable {

        private ConcurrentSocket(
            final DatagramSocket socket,
            final ExecutorService executor
        ) throws SocketException {
            this(socket, executor, new byte[socket.getReceiveBufferSize()]);
        }

        @Override
        public void close() {
            socket.close();
            executor.close();
        }
    }
}
