package info.kgeorgiy.ja.serov.lambda.spliterator;

import info.kgeorgiy.java.advanced.lambda.Trees;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public abstract class TreeSpliterator<T, V> implements Spliterator<V> {

    private static final int LEAF_CHARACTERISTICS = SIZED | SUBSIZED;
    private static final int NO_CHARACTERISTICS = 0;

    private final int characteristics;

    private Deque<T> order;
    private int consumed;

    protected TreeSpliterator(final T root) {
        this.order = new ArrayDeque<>();
        this.characteristics = characteristicsIf(root);
        order.addLast(root);
    }

    protected TreeSpliterator(final Deque<T> order) {
        this.order = order;
        this.characteristics = order.size() == 1 ? characteristicsIf(order.getLast()) : NO_CHARACTERISTICS;
    }

    private static int characteristicsIf(final Object tree) {
        return tree instanceof Trees.Leaf ? LEAF_CHARACTERISTICS : NO_CHARACTERISTICS;
    }

    @SafeVarargs
    protected final boolean acceptNodes(final T... nodes) {
        return acceptNodes(List.of(nodes));
    }

    protected final boolean acceptNodes(final Collection<T> nodes) {
        order.addAll(nodes);
        return false;
    }

    protected boolean acceptLeaf(final Trees.Leaf<V> leaf, final Consumer<Trees.Leaf<V>> leafAction) {
        leafAction.accept(leaf);
        return true;
    }

    private void acceptLeafValue(final Trees.Leaf<V> leaf, final Consumer<? super V> action) {
        action.accept(leaf.value());
        consumed++;
    }

    // === Abstracts

    protected abstract boolean acceptTree(T tree, Consumer<Trees.Leaf<V>> leafAction);

    protected abstract T cast(Trees.Leaf<V> leaf);

    protected abstract Spliterator<V> ofOrder(Deque<T> order);

    protected abstract long estimateCapacity(int depth);

    // === End

    private boolean updateOrder(final Consumer<Trees.Leaf<V>> leafAction) {
        while (!order.isEmpty()) {
            if (acceptTree(order.removeLast(), leafAction)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super V> action) {
        return updateOrder(leaf -> acceptLeafValue(leaf, action));
    }

    @Override
    public Spliterator<V> trySplit() {
        updateOrder(leaf -> order.addLast(cast(leaf)));
        if (order.size() < 2) {
            return null;
        }

        final Deque<T> first = new ArrayDeque<>();
        first.addLast(order.removeFirst());

        final Spliterator<V> prefix = ofOrder(order);
        order = first;
        return prefix;
    }

    @Override
    public long estimateSize() {
        return estimateCapacity(order.size()) - consumed;
    }

    @Override
    @SuppressWarnings("MagicConstant")
    public int characteristics() {
        return IMMUTABLE | ORDERED | characteristics;
    }
}
