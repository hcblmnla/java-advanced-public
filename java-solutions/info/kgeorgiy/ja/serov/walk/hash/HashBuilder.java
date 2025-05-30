package info.kgeorgiy.ja.serov.walk.hash;

/**
 * A class that consistently builds a hash.
 *
 * @param <T> hash representation type
 * @author alnmlbch
 */
public interface HashBuilder<T> {

    /**
     * A neutral (empty) hash. Resets the current progress.
     *
     * @return empty hash value
     */
    T getEmptyHash();

    /**
     * The built hash getter. Resets the current progress.
     *
     * @return built hash value
     */
    T getHash();

    /**
     * Updates the hash value.
     *
     * @param size  bytes count to be hashed
     * @param bytes bytes
     */
    void update(int size, byte... bytes);
}
