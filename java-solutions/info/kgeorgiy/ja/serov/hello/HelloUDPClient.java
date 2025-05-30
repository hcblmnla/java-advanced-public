package info.kgeorgiy.ja.serov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Blocking UDP implementation of {@link AbstractHelloUDPClient abstract UDP client}.
 *
 * @author alnmlbch
 */
public class HelloUDPClient extends AbstractHelloUDPClient {

    /** Default constructor with no arguments. */
    public HelloUDPClient() {
        super(new ConcurrentHashMap<>());
    }

    /** {@link HelloUDPClient} launcher. */
    public static void main(final String... args) {
        invoke(new HelloUDPClient(), args);
    }

    @Override
    public void newRun(final List<Request> requests, final int threads) {
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            IntStream
                .rangeClosed(1, threads)
                .mapToObj(Integer::toString)
                .forEach(thread -> executor.submit(() -> requests.forEach(request -> {
                    final SocketAddress address = addresses.computeIfAbsent(
                        request,
                        req -> new InetSocketAddress(req.host(), req.port())
                    );
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.setSoTimeout(TIMEOUT_IN_MILLIS);

                        final byte[] buffer = new byte[socket.getReceiveBufferSize()];
                        final DatagramPacket received = new DatagramPacket(buffer, buffer.length);

                        final String message = HelloUtils.encodeMessage(
                            request.template(),
                            thread
                        );

                        System.out.println(exchange(
                            socket,
                            HelloUtils.asPacket(message, address),
                            received,
                            message
                        ));
                    } catch (final SocketException e) {
                        System.err.println("Socket client error occurred: " + e.getMessage());
                    }
                })));
        }
    }

    private String exchange(
        final DatagramSocket socket,
        final DatagramPacket packet,
        final DatagramPacket received,
        final String message
    ) throws SocketException {
        while (!socket.isClosed()) {
            try {
                socket.send(packet);
                socket.receive(received);
                final String response = HelloUtils.asString(received);
                if (test(response, message)) {
                    return response;
                }
            } catch (final IOException e) {
                System.err.println("Request client error occurred: " + e.getMessage());
            }
        }
        throw new SocketException("Socket " + socket + " is closed");
    }
}
