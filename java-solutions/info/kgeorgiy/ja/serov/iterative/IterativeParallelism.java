package info.kgeorgiy.ja.serov.iterative;

import info.kgeorgiy.java.advanced.iterative.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * {@link ScalarIP Scalar iterative parallelism} implementation.
 * <p>
 * Provides an API for working with lists in multithreaded mode.
 *
 * @author alnmlbch
 */
public class IterativeParallelism implements ScalarIP {

    // npos index
    private static final int NO_INDEX = -1;
    private final ParallelMapper parallelMapper;

    /**
     * Public default constructor.
     * <p>
     * All next invocations will create new threads instances.
     */
    public IterativeParallelism() {
        this(null);
    }

    /**
     * {@link IterativeParallelism IP} constructor using {@link ParallelMapper parallel mapper}.
     * <p>
     * Provides that no new threads will be created.
     * It is possible to run multiple {@link IterativeParallelism IP} instances.
     *
     * @param parallelMapper given parallel mapper implementation
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /** {@link IterativeParallelism Iterative parallelism} launcher. */
    public static void main(final String[] args) {
        // ip
        final ScalarIP ip = new IterativeParallelism();
        // ip + mapper
        try (final ParallelMapper parallelMapper = new ParallelMapperImpl(
            Runtime.getRuntime().availableProcessors()
        )) {
            final ScalarIP ipm = new IterativeParallelism(parallelMapper);
            // usage
            System.out.println(List.of(ip, ipm));
        }
    }

    @Override
    public <T> int argMax(
        final int threads,
        final List<T> values,
        final Comparator<? super T> comparator
    ) throws InterruptedException {
        final Function<Stream<Indexed<T>>, Indexed<T>> evaluator =
            stream -> stream
                .max(Comparator.comparing(Indexed::value, comparator))
                .orElseThrow(NoSuchElementException::new);
        return parallel(threads, values, evaluator, evaluator.andThen(Indexed::index));
    }

    @Override
    public <T> int argMin(
        final int threads,
        final List<T> values,
        final Comparator<? super T> comparator
    ) throws InterruptedException {
        return argMax(threads, values, comparator.reversed());
    }

    private <T> IntStream filteredIndices(
        final Stream<Indexed<T>> stream,
        final Predicate<? super T> predicate
    ) {
        return stream
            .filter(i -> predicate.test(i.value()))
            .mapToInt(Indexed::index);
    }

    private int findFirstIndex(final IntStream indices) {
        return indices.findFirst().orElse(NO_INDEX);
    }

    @Override
    public <T> int indexOf(
        final int threads,
        final List<T> values,
        final Predicate<? super T> predicate
    ) throws InterruptedException {
        return parallel(
            threads,
            values,
            chunk -> findFirstIndex(filteredIndices(chunk, predicate)),
            results -> findFirstIndex(
                results
                    .mapToInt(index -> index)
                    .filter(index -> index != NO_INDEX)
            )
        );
    }

    @Override
    public <T> int lastIndexOf(
        final int threads,
        final List<T> values,
        final Predicate<? super T> predicate
    ) throws InterruptedException {
        final int reversedIndex = indexOf(threads, values.reversed(), predicate);
        return reversedIndex == NO_INDEX ? NO_INDEX : values.size() - 1 - reversedIndex;
    }

    @Override
    public <T> long sumIndices(
        final int threads,
        final List<? extends T> values,
        final Predicate<? super T> predicate
    ) throws InterruptedException {
        return parallel(
            threads,
            values,
            chunk -> filteredIndices(chunk, predicate).mapToLong(index -> index).sum(),
            results -> results.reduce(0L, Long::sum)
        );
    }

    private <T> Stream<OffsetList<T>> chunked(
        final int chunks,
        final List<? extends T> values
    ) {
        final int nChunks = Math.min(chunks, values.size());
        final int chunkSize = values.size() / nChunks;
        final int rem = values.size() % nChunks;
        return IntStream
            .range(0, nChunks)
            .mapToObj(i -> {
                final int start = i * chunkSize + Math.min(i, rem);
                final int end = start + chunkSize + (i < rem ? 1 : 0);
                return new OffsetList<>(values.subList(start, end), start);
            });
    }

    private <T, A, R> R parallel(
        final int threads,
        final List<? extends T> values,
        final Function<Stream<Indexed<T>>, ? extends A> chunkF,
        final Function<Stream<A>, ? extends R> finalizer
    ) throws InterruptedException {
        final Stream<OffsetList<T>> chunks = chunked(threads, values);
        final Function<OffsetList<T>, A> chunkFunction =
            list -> chunkF.apply(
                IntStream.range(0, list.view().size())
                    .mapToObj(i -> new Indexed<>(list.view().get(i), list.offset() + i))
            );
        return parallelMapper == null
            ? parallel(chunks, chunkFunction, finalizer)
            : finalizer.apply(parallelMapper.map(chunkFunction, chunks.toList()).stream());
    }

    private <T, A, R> R parallel(
        final Stream<OffsetList<T>> chunks,
        final Function<OffsetList<T>, A> chunkFunction,
        final Function<Stream<A>, ? extends R> finalizer
    ) throws InterruptedException {
        final List<ThreadValue<A>> threadValues = chunks
            .map(chunk -> new ThreadValue<>(() -> chunkFunction.apply(chunk)))
            .toList();
        ParallelUtils.join(
            threadValues.stream()
                .map(ThreadValue::thread)
                .toList()
        );
        return finalizer.apply(threadValues.stream().map(ThreadValue::value));
    }

    private record OffsetList<T>(List<? extends T> view, int offset) {
    }

    private record Indexed<T>(T value, int index) {
    }

    private static class ThreadValue<T> {
        private final Thread thread;
        private T value;

        private ThreadValue(final Supplier<? extends T> supplier) {
            this.thread = Thread.ofPlatform().start(() -> this.value = supplier.get());
        }

        private Thread thread() {
            return thread;
        }

        private T value() {
            if (thread.isAlive()) {
                throw new IllegalStateException("Thread (" + thread + ") is alive");
            }
            return value;
        }
    }
}
