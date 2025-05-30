package info.kgeorgiy.ja.serov.hello;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Nonblocking UDP implementation of {@link AbstractHelloUDPClient abstract UDP client}.
 *
 * @author alnmlbch
 */
public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {

    /** Default constructor with no arguments. */
    public HelloUDPNonblockingClient() {
        super(new HashMap<>());
    }

    /** {@link HelloUDPNonblockingClient} launcher. */
    public static void main(final String... args) {
        invoke(new HelloUDPNonblockingClient(), args);
    }

    @Override
    public void newRun(final List<Request> requests, final int threads) {
        try (Runner runner = new Runner(requests, threads, Selector.open())) {
            final AtomicInteger completed = new AtomicInteger();
            final int totalRequests = threads * requests.size();

            while (completed.intValue() < totalRequests) {
                runner.accept(completed);
            }
        } catch (final IOException e) {
            System.err.println("Failed to open selector");
        }
    }

    private class Runner implements Consumer<AtomicInteger>, Closeable {
        private final List<Request> requests;
        private final Selector selector;

        private final Set<Context> contexts;

        private Runner(
            final List<Request> requests,
            final int threads,
            final Selector selector
        ) {
            this.requests = requests;
            this.selector = selector;

            this.contexts = IntStream
                .rangeClosed(1, threads)
                .mapToObj(Integer::toString)
                .map(thread -> {
                    try {
                        final DatagramChannel channel = DatagramChannel.open();
                        channel.configureBlocking(false);
                        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

                        final Context context = new Context(channel, thread);
                        context.update();

                        channel.register(selector, SelectionKey.OP_WRITE, context);
                        return context;
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .collect(Collectors.toSet());
        }

        @Override
        public void accept(final AtomicInteger completed) {
            try {
                if (selector.select(TIMEOUT_IN_MILLIS) == 0) {
                    contexts.stream()
                        .filter(context -> !context.completed)
                        .forEach(Context::trySendRequest);
                    return;
                }
                HelloNonblockingUtils.forEachSelectionKey(
                    selector,
                    Map.of(
                        SelectionKey::isReadable,
                        (key, att) -> {
                            final Context context = (Context) att;
                            final Optional<String> optResponse = context.receiveResponse();
                            if (optResponse.isEmpty()) {
                                return;
                            }
                            final String response = optResponse.get();
                            if (!test(response, context.message)) {
                                return;
                            }
                            System.out.println(response);
                            context.markCompleted();
                            completed.incrementAndGet();
                            if (context.done()) {
                                context.cleanUp(key);
                                contexts.remove(context);
                            } else {
                                context.update();
                                context.trySendRequest();
                            }
                        },
                        SelectionKey::isWritable,
                        (key, att) -> {
                            if (((Context) att).trySendRequest() > 0) {
                                key.interestOps(SelectionKey.OP_READ);
                            }
                        }
                    ),
                    (key, att, ioe) -> {
                        ((Context) att).cleanUp(key);
                        System.err.println("IO error occurred: " + ioe.getMessage());
                    }
                );
            } catch (final IOException e) {
                System.err.println("Select error occurred: " + e.getMessage());
            }
        }

        @Override
        public void close() {
            contexts.forEach(Context::close);
        }

        private class Context implements Closeable {
            private final DatagramChannel channel;
            private final String thread;
            private final ByteBuffer buffer;

            private int request = 0;
            private SocketAddress address;
            private String message;
            private boolean completed = false;

            private Context(final DatagramChannel channel, final String thread) {
                this.buffer = ByteBuffer.allocate(HelloNonblockingUtils.BUFFER_SIZE);
                this.channel = channel;
                this.thread = thread;
            }

            private boolean done() {
                return request >= requests.size();
            }

            private void update() {
                if (done()) {
                    return;
                }
                final Request req = requests.get(request++);
                address = addresses.computeIfAbsent(
                    req, r -> new InetSocketAddress(r.host(), r.port())
                );
                message = HelloUtils.encodeMessage(req.template(), thread);
                completed = false;
            }

            private void markCompleted() {
                completed = true;
            }

            private int trySendRequest() {
                buffer.clear()
                    .put(message.getBytes(StandardCharsets.UTF_8))
                    .flip();
                try {
                    return channel.send(buffer, address);
                } catch (final IOException e) {
                    System.err.println("Failed to send request: " + e.getMessage());
                    return 0;
                }
            }

            private Optional<String> receiveResponse() throws IOException {
                buffer.clear();
                return Optional.ofNullable(channel.receive(buffer))
                    .map(_ -> {
                        buffer.flip();
                        // or else BufferUnderflowException
                        final byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    });
            }

            private void cleanUp(final SelectionKey key) {
                key.cancel();
                close();
            }

            @Override
            public void close() {
                try {
                    channel.close();
                } catch (final IOException e) { // simplification
                    System.err.println("Error closing channel: " + e);
                }
            }

            @Override
            public int hashCode() {
                return Objects.hash(channel, thread);
            }
        }
    }
}
