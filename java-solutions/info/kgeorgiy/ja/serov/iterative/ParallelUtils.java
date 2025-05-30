package info.kgeorgiy.ja.serov.iterative;

import java.util.Collection;

/**
 * Useful utilities for parallel programming.
 *
 * @author alnmlbch
 */
public enum ParallelUtils {
    ;

    /**
     * Safety thread's joiner.
     * <p>
     * Tries to join all threads and collect given stacktrace.
     *
     * @param threads list of the threads
     * @throws InterruptedException if at least one thread was interrupted
     */
    public static void join(final Collection<Thread> threads) throws InterruptedException {
        final InterruptedException accumulatorE = new InterruptedException();
        boolean interrupted = false;
        for (final Thread thread : threads) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException e) {
                    accumulatorE.addSuppressed(e);
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw accumulatorE;
        }
    }
}
