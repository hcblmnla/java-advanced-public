package info.kgeorgiy.ja.serov.lambda;

import info.kgeorgiy.ja.serov.lambda.spliterator.TreeSpliterator;
import info.kgeorgiy.java.advanced.lambda.EasyLambda;
import info.kgeorgiy.java.advanced.lambda.Trees;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

public class Lambda implements EasyLambda {

    private static final String NO_COMMON_AFFIX = "";

    // === Spliterators

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(final Trees.Binary<T> tree) {
        class Splitter extends TreeSpliterator<Trees.Binary<T>, T> {

            protected Splitter(final Trees.Binary<T> root) {
                super(root);
            }

            protected Splitter(final Deque<Trees.Binary<T>> order) {
                super(order);
            }

            @Override
            protected boolean acceptTree(
                final Trees.Binary<T> tree,
                final Consumer<Trees.Leaf<T>> leafAction
            ) {
                return switch (tree) {
                    case Trees.Binary.Branch<T>(
                        final Trees.Binary<T> left,
                        final Trees.Binary<T> right
                    ) -> acceptNodes(right, left);
                    case final Trees.Leaf<T> leaf -> acceptLeaf(leaf, leafAction);
                };
            }

            @Override
            protected Trees.Binary<T> cast(final Trees.Leaf<T> leaf) {
                return leaf;
            }

            @Override
            protected Spliterator<T> ofOrder(final Deque<Trees.Binary<T>> order) {
                return new Splitter(order);
            }

            @Override
            protected long estimateCapacity(final int depth) {
                return 1L << depth;
            }
        }
        return new Splitter(tree);
    }

    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(final Trees.SizedBinary<T> tree) {
        class Splitter extends TreeSpliterator<Trees.SizedBinary<T>, T> {

            private long capacity;

            protected Splitter(final Trees.SizedBinary<T> root) {
                super(root);
                this.capacity = root.size();
            }

            protected Splitter(final Deque<Trees.SizedBinary<T>> order) {
                super(order);
                this.capacity = order.stream().mapToInt(Trees.SizedBinary::size).sum();
            }

            @Override
            protected boolean acceptTree(
                final Trees.SizedBinary<T> tree,
                final Consumer<Trees.Leaf<T>> leafAction
            ) {
                return switch (tree) {
                    case Trees.SizedBinary.Branch<T>(
                        final Trees.SizedBinary<T> left,
                        final Trees.SizedBinary<T> right,
                        final int _
                    ) -> acceptNodes(right, left);
                    case final Trees.Leaf<T> leaf -> acceptLeaf(leaf, leafAction);
                };
            }

            @Override
            protected Trees.SizedBinary<T> cast(final Trees.Leaf<T> leaf) {
                return leaf;
            }

            @Override
            protected Spliterator<T> ofOrder(final Deque<Trees.SizedBinary<T>> order) {
                return new Splitter(order);
            }

            @Override
            public Spliterator<T> trySplit() {
                final Spliterator<T> prefix = super.trySplit();
                if (prefix != null) {
                    capacity -= prefix.estimateSize();
                }
                return prefix;
            }

            @Override
            protected long estimateCapacity(final int depth) {
                return capacity;
            }

            @Override
            public int characteristics() {
                return super.characteristics() | SIZED | SUBSIZED;
            }
        }
        return new Splitter(tree);
    }

    @Override
    public <T> Spliterator<T> naryTreeSpliterator(final Trees.Nary<T> tree) {
        class Splitter extends TreeSpliterator<Trees.Nary<T>, T> {

            private final int ary;

            protected Splitter(final Trees.Nary<T> root) {
                super(root);
                this.ary = aryOfTree(root);
            }

            protected Splitter(final Deque<Trees.Nary<T>> order) {
                super(order);
                this.ary = order.isEmpty() ? 1 : aryOfTree(order.getFirst());
            }

            private static <V> int aryOfTree(final Trees.Nary<V> tree) {
                return switch (tree) {
                    case Trees.Nary.Node<V>(final List<Trees.Nary<V>> children) -> children.size();
                    case final Trees.Leaf<V> _ -> 1;
                };
            }

            @Override
            protected boolean acceptTree(
                final Trees.Nary<T> tree,
                final Consumer<Trees.Leaf<T>> leafAction
            ) {
                return switch (tree) {
                    case Trees.Nary.Node<T>(
                        final List<Trees.Nary<T>> children
                    ) -> acceptNodes(children.reversed());
                    case final Trees.Leaf<T> leaf -> acceptLeaf(leaf, leafAction);
                };
            }

            @Override
            protected Trees.Nary<T> cast(final Trees.Leaf<T> leaf) {
                return leaf;
            }

            @Override
            protected Spliterator<T> ofOrder(final Deque<Trees.Nary<T>> order) {
                return new Splitter(order);
            }

            @Override
            protected long estimateCapacity(final int depth) {
                return (long) Math.pow(ary, depth);
            }
        }
        return new Splitter(tree);
    }

    // === Extreme

    private <T, A> Collector<T, ?, Optional<T>> notParallel(
        final Supplier<A> supplier,
        final BiConsumer<A, T> accumulator,
        final Function<A, Optional<T>> finisher,
        final String method
    ) {
        final BinaryOperator<A> combiner = (_, _) -> {
            /* return state1; */
            throw new UnsupportedOperationException(method + " cannot be split");
        };
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    private <T> Collector<T, ?, Optional<T>> extreme(
        final BiConsumer<AtomicValue<T>, T> accumulator,
        final String method
    ) {
        return notParallel(
            AtomicValue::new,
            accumulator,
            state -> Optional.ofNullable(state.value),
            method
        );
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return extreme(
            (state, e) -> {
                if (state.value == null) {
                    state.value = e;
                }
            },
            "first"
        );
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return extreme((state, e) -> state.value = e, "last");
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        class DequeState {

            private static final IntPredicate ODD_GREATER_THAN_ONE = n -> n % 2 == 1 && n > 1;
            private static final IntPredicate EVEN_GREATER_THAN_ZERO = n -> n % 2 == 0 && n > 0;

            private final Deque<T> deque = new ArrayDeque<>();
            private int flag = 0;

            private void removeFirstIf(final IntPredicate predicate) {
                if (predicate.test(flag)) {
                    deque.removeFirst();
                }
            }
        }

        return notParallel(
            DequeState::new,
            (state, e) -> {
                state.deque.addLast(e);
                state.flag++;
                state.removeFirstIf(DequeState.ODD_GREATER_THAN_ONE);
            },
            state -> {
                state.removeFirstIf(DequeState.EVEN_GREATER_THAN_ZERO);
                return Optional.ofNullable(state.deque.peekFirst());
            },
            "middle"
        );
    }

    // === Strings

    private Collector<CharSequence, ?, String> commonAffix(
        final UnaryOperator<StringBuilder> initializer,
        final ToIntFunction<CharSequence> toSeedFunction,
        final int step
    ) {
        final BiConsumer<StringBuilder, CharSequence> affixUpdater = (receiver, provider) -> {
            final int seed = toSeedFunction.applyAsInt(provider);
            for (int i = 0, j = seed; i < receiver.length(); i++, j += step) {
                if (i == provider.length() || receiver.charAt(i) != provider.charAt(j)) {
                    receiver.setLength(i);
                    return;
                }
            }
        };

        return Collector.<CharSequence, AtomicValue<StringBuilder>, String>of(
            AtomicValue::new,
            (state, seq) -> {
                if (state.value == null) {
                    state.value = initializer.apply(new StringBuilder(seq));
                    return;
                }
                affixUpdater.accept(state.value, seq);
            },
            (state1, state2) -> {
                affixUpdater.accept(state1.value, state2.value);
                return state1;
            },
            state -> Optional.ofNullable(state.value)
                .map(initializer)
                .map(StringBuilder::toString)
                .orElse(NO_COMMON_AFFIX)
        );
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return commonAffix(UnaryOperator.identity(), _ -> 0, 1);
    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return commonAffix(StringBuilder::reverse, seq -> seq.length() - 1, -1);
    }

    private static class AtomicValue<V> {
        private /* volatile */ V value;
    }
}
