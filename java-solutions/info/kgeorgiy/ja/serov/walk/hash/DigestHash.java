package info.kgeorgiy.ja.serov.walk.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public class DigestHash extends StringHashBuilder {

    private final MessageDigest md;
    private final int size;

    protected DigestHash(final String algorithm, final int size) throws NoSuchAlgorithmException {
        super("%%0%dx".formatted(size));
        this.md = MessageDigest.getInstance(algorithm.toUpperCase());
        this.size = size / 2;
    }

    @Override
    public String getEmptyHash() {
        md.reset();
        return getZeroHash();
    }

    @Override
    public String getHash() {
        return HexFormat.of()
            .formatHex(Arrays.copyOf(md.digest(), size));
    }

    @Override
    public void update(final int size, final byte... bytes) {
        md.update(Arrays.copyOf(bytes, size));
    }
}
