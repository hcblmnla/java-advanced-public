package info.kgeorgiy.ja.serov.arrayset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum Utils {
    ;

    public static void main(final String[] args) {
        System.out.println(new ArraySet<>().size());
    }

    private static <E> List<E> toSortedDistinctArrayList(
        final Collection<? extends E> collection,
        final Comparator<? super E> comparator
    ) {
        final List<E> sorted = new ArrayList<>(collection);
        sorted.sort(comparator);
        if (comparator == null || sorted.size() <= 1) {
            return sorted;
        }
        final List<E> distinct = new ArrayList<>();
        E cur = sorted.getFirst();
        distinct.add(cur);
        for (final E e : sorted.subList(1, sorted.size())) {
            if (comparator.compare(cur, e) != 0) {
                distinct.add(e);
                cur = e;
            }
        }
        return distinct;
    }

    public static <E> List<E> toSortedDistinctList(
        final Collection<? extends E> collection,
        final Comparator<? super E> comparator
    ) {
        return Collections.unmodifiableList(
            toSortedDistinctArrayList(collection, comparator)
        );
    }
}
