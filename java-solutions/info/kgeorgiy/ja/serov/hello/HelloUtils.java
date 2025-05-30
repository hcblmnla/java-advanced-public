package info.kgeorgiy.ja.serov.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Useful utilities for net hello UDP programming.
 *
 * @author alnmlbch
 */
public enum HelloUtils {
    ;

    private static final String ANSWER_PREFIX = "Hello, ";
    private static final Pattern KEY = Pattern.compile("\\$");

    /**
     * Maps {@link DatagramPacket UDP packet} to {@link String string}.
     *
     * @param packet given UDP packet
     * @return mapped string
     */
    public static String asString(final DatagramPacket packet) {
        final String template = new String(
            packet.getData(),
            packet.getOffset(),
            packet.getLength(),
            StandardCharsets.UTF_8
        );
        return ANSWER_PREFIX + template;
    }

    /**
     * Maps {@link String string} to {@link DatagramPacket UDP packet}.
     * Using specified {@link SocketAddress socket address}.
     *
     * @param string  given string
     * @param address specified socket address
     * @return mapped UDP packet
     */
    public static DatagramPacket asPacket(final String string, final SocketAddress address) {
        final byte[] data = string.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(data, data.length, address);
    }

    /**
     * Encodes given messages' template.
     *
     * @param template string template
     * @param value    replacement value
     * @return encoded message
     */
    public static String encodeMessage(final String template, final String value) {
        return KEY.matcher(template).replaceAll(value);
    }
}
