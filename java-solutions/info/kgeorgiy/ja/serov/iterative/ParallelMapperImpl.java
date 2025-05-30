package info.kgeorgiy.ja.serov.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * {@link ParallelMapper Parallel mapper} simple implementation
 * no using {@link java.util.concurrent Concurrency Utilities}.
 *
 * @author alnmlbch
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final TaskQueue tasks;
    private final List<Thread> threads;

    private volatile boolean closed = false;

    /**
     * Creates new {@link ParallelMapperImpl} instance.
     * <p>
     * All {@code n} threads will be used
     * in next invocations {@link ParallelMapper#map(Function, List)}.
     *
     * @param threads number of using threads
     */
    public ParallelMapperImpl(final int threads) {
        this.tasks = new TaskQueue();
        this.threads = IntStream.range(0, threads)
            .mapToObj(_ -> Thread.ofPlatform().start(() -> {
                while (!Thread.interrupted() && !closed) {
                    try {
                        tasks.removeTask().run();
                    } catch (final InterruptedException _) {
                        // okay
                    }
                }
            }))
            .toList();
    }

    @Override
    public <T, R> List<R> map(
        final Function<? super T, ? extends R> f,
        final List<? extends T> items
    ) throws InterruptedException {
        if (closed) {
            throw new IllegalStateException("Mapper is closed");
        }
        final List<R> results = new ArrayList<>(Collections.nCopies(items.size(), null));
        final List<Task> localTasks = IntStream.range(0, items.size())
            .mapToObj(i -> new Task(() -> results.set(i, f.apply(items.get(i)))))
            .peek(tasks::addTask)
            .toList();
        for (final Task task : localTasks) {
            task.tryComplete();
        }
        return results;
    }

    /**
     * Stops all threads.
     * <p>
     * This is easy version that means all unfinished mappings are left in undefined state.
     */
    @Override
    public void close() {
        closed = true;
        try {
            threads.forEach(Thread::interrupt);
            ParallelUtils.join(threads);
        } catch (final InterruptedException e) {
            // joined but threw, okay
        }
        tasks.clear();
    }

    private static class TaskQueue {

        private final Queue<Task> queue = new ArrayDeque<>();

        private synchronized Task removeTask() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            final Task task = queue.remove();
            notify();
            return task;
        }

        private synchronized void addTask(final Task task) {
            queue.add(task);
            notify();
        }

        private synchronized void clear() {
            queue.clear();
        }
    }

    private static class Task {

        private final Runnable task;
        private boolean done;

        private Task(final Runnable task) {
            this.task = task;
        }

        private synchronized void run() {
            try {
                task.run();
            } catch (final RuntimeException e) {
                System.err.format(
                    "In thread %s task threw an exception: %s%n",
                    Thread.currentThread().getName(),
                    e.getMessage()
                );
            }
            done = true;
            notify();
        }

        private synchronized void tryComplete() throws InterruptedException {
            while (!done) {
                wait();
            }
        }
    }
}
