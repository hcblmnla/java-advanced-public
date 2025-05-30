package info.kgeorgiy.ja.serov.arrayset;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final List<E> elements;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(List.of());
    }

    public ArraySet(final Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(final Collection<? extends E> collection, final Comparator<? super E> comparator) {
        this(Utils.toSortedDistinctList(collection, comparator), comparator);
    }

    private ArraySet(final List<E> elements, final Comparator<? super E> comparator) {
        this.elements = elements;
        this.comparator = comparator;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    private int binarySearch(final E element) {
        return Collections.binarySearch(elements, element, comparator);
    }

    private int indexOf(final E element) {
        final int index = binarySearch(element);
        return index < 0 ? -index - 1 : index;
    }

    private SortedSet<E> subSet(final int fromIndex, final int toIndex) {
        return new ArraySet<>(elements.subList(fromIndex, toIndex), comparator);
    }

    @SuppressWarnings("unchecked")
    private int compare(final E e1, final E e2) {
        return comparator == null
            ? ((Comparable<? super E>) e1).compareTo(e2)
            : comparator.compare(e1, e2);
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("'fromElement' cannot be greater than 'toElement'");
        }
        return subSet(indexOf(fromElement), indexOf(toElement));
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return subSet(0, indexOf(toElement));
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return subSet(indexOf(fromElement), size());
    }

    @Override
    public E first() {
        return elements.getFirst();
    }

    @Override
    public E last() {
        return elements.getLast();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return binarySearch((E) o) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }
}
