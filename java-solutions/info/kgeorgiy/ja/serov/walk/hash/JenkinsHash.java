package info.kgeorgiy.ja.serov.walk.hash;

public class JenkinsHash extends StringHashBuilder {

    private int hash;

    public JenkinsHash() {
        super("%08x");
    }

    @Override
    public String getEmptyHash() {
        hash = 0;
        return getZeroHash();
    }

    @Override
    public String getHash() {
        hash += hash << 3;
        hash ^= hash >>> 11;
        hash += hash << 15;
        final int result = hash;
        hash = 0;
        return getHashFromInt(result);
    }

    @Override
    public void update(final int size, final byte... bytes) {
        for (int i = 0; i < size; i++) {
            hash += bytes[i] & 0xff;
            hash += hash << 10;
            hash ^= hash >>> 6;
        }
    }
}
