package info.kgeorgiy.ja.serov.hello;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Nonblocking UDP implementation of {@link AbstractHelloUDPServer abstract UDP server}.
 *
 * @author alnmlbch
 */
public class HelloUDPNonblockingServer
    extends AbstractHelloUDPServer<HelloUDPNonblockingServer.Context> {

    private static final int QUEUE_CAPACITY = 1024;
    private static final int TIMEOUT_IN_MILLIS = 10;

    private final ExecutorService engine = Executors.newCachedThreadPool();

    /** Default constructor with no arguments. */
    public HelloUDPNonblockingServer() {
        super(new ConcurrentHashMap<>());
    }

    /** {@link HelloUDPNonblockingServer} launcher. */
    public static void main(final String... args) {
        invoke(HelloUDPNonblockingServer::new, args);
    }

    @Override
    @SuppressWarnings("resource")
    public void startSafely(final int port, final int threads) {
        final Context context = contexts.computeIfAbsent(port, p -> {
            try {
                return initContext(p, threads);
            } catch (final IOException e) {
                close();
                throw new UncheckedIOException(e);
            }
        });
        engine.submit(() -> context.accept(running));
    }

    private Context initContext(final int port, final int threads) throws IOException {
        final Selector selector = Selector.open();
        final DatagramChannel channel = DatagramChannel.open();

        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.bind(new InetSocketAddress(port));

        final SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final var responses = new ArrayBlockingQueue<Context.Response>(QUEUE_CAPACITY);

        final Context context = new Context(selector, channel, executor, responses);
        key.attach(context);
        return context;
    }

    @Override
    public void close() {
        super.close();
        engine.close();
    }

    /**
     * Helper nonblocking server context.
     *
     * @param selector  given selector
     * @param channel   UDP channel
     * @param executor  parallel executor
     * @param responses response queue implementation
     * @author alnmlbch
     */
    public record Context(
        Selector selector,
        DatagramChannel channel,
        ExecutorService executor,
        ArrayBlockingQueue<Response> responses
    ) implements Consumer<AtomicBoolean>, Closeable {

        private void processRequest(final ByteBuffer buffer, final SocketAddress client) {
            final byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            final DatagramPacket packet = new DatagramPacket(data, data.length);
            final String response = HelloUtils.asString(packet);

            responses.add(new Response(client, response));
        }

        @Override
        public void accept(final AtomicBoolean running) {
            final ByteBuffer buffer = ByteBuffer.allocate(HelloNonblockingUtils.BUFFER_SIZE);
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                if (!responses.isEmpty()) {
                    final List<Response> batch = new ArrayList<>();
                    responses.drainTo(batch);
                    batch.forEach(response -> {
                        try {
                            final ByteBuffer responseBuffer = ByteBuffer.wrap(
                                response.message().getBytes(StandardCharsets.UTF_8)
                            );
                            channel.send(responseBuffer, response.client());
                        } catch (final IOException e) {
                            System.err.println("Failed to send response: " + e.getMessage());
                        }
                    });
                }
                try {
                    if (selector.select(TIMEOUT_IN_MILLIS) == 0) {
                        continue;
                    }
                    HelloNonblockingUtils.forEachSelectionKey(
                        selector,
                        Map.of(
                            SelectionKey::isReadable,
                            (_, _) -> {
                                buffer.clear();
                                final SocketAddress client = channel.receive(buffer);
                                if (client == null) {
                                    return;
                                }
                                final ByteBuffer requestBuffer = ByteBuffer
                                    .allocate(buffer.flip().remaining())
                                    .put(buffer)
                                    .flip();
                                executor.submit(() -> processRequest(requestBuffer, client));
                            }
                        )
                    );
                } catch (final IOException e) {
                    System.err.println("Server error occurred: " + e.getMessage());
                } catch (final ClosedSelectorException e) {
                    System.err.println("Selector is closed");
                }
            }
        }

        @Override
        public void close() throws IOException {
            IOException thrown = null;
            try {
                selector.close();
            } catch (final IOException e) {
                thrown = e;
            }
            try {
                channel.close();
            } catch (final IOException e) {
                if (thrown != null) {
                    e.addSuppressed(thrown);
                }
                throw e;
            } finally {
                executor.close();
                responses.clear();
            }
            if (thrown != null) {
                throw thrown;
            }
        }

        private record Response(SocketAddress client, String message) {
        }
    }
}
