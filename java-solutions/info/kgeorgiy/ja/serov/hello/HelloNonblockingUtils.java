package info.kgeorgiy.ja.serov.hello;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Useful utilities for nonblocking hello UDP programming.
 *
 * @author alnmlbch
 */
public enum HelloNonblockingUtils {
    ;

    /** Common cool buffer size. */
    public static final int BUFFER_SIZE = 1024;

    private static void walkKeys(
        final Map<Predicate<SelectionKey>, SelectionKeyConsumerE> actions,
        final SelectionKey key,
        final Object attachment
    ) throws IOException {
        for (final var entry : actions.entrySet()) {
            if (entry.getKey().test(key)) {
                entry.getValue().accept(key, attachment);
                return;
            }
        }
    }

    /**
     * Handles each valid {@link SelectionKey key}.
     *
     * @param selector given selector
     * @param actions  predicates and actions for each key
     * @param cleanups cleanup action holder
     * @throws IOException if error occurred
     */
    public static void forEachSelectionKey(
        final Selector selector,
        final Map<Predicate<SelectionKey>, SelectionKeyConsumerE> actions,
        final Cleaner... cleanups
    ) throws IOException {
        final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            final SelectionKey key = iterator.next();
            iterator.remove();
            if (!key.isValid()) {
                continue;
            }
            final Object attachment = key.attachment();
            if (cleanups.length == 0) {
                walkKeys(actions, key, attachment);
                return;
            }
            try {
                walkKeys(actions, key, attachment);
            } catch (final IOException e) {
                cleanups[0].clean(key, attachment, e);
            }
        }
    }

    /**
     * {@link SelectionKey} handler.
     *
     * @author alnmlbch
     */
    @FunctionalInterface
    public interface SelectionKeyConsumerE {

        /**
         * Accepts key and throws an exception.
         *
         * @param key selection key
         * @param att key attachment
         * @throws IOException if error occurred
         */
        void accept(SelectionKey key, Object att) throws IOException;
    }

    /**
     * {@link SelectionKey} cleaner.
     *
     * @author alnmlbch
     */
    @FunctionalInterface
    public interface Cleaner {

        /**
         * Accepts key, attachment and thrown exception and cleans.
         *
         * @param key selection key
         * @param att key attachment
         * @param ioe thrown exception
         */
        void clean(SelectionKey key, Object att, IOException ioe);
    }
}
