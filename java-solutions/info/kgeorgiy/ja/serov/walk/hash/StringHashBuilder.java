package info.kgeorgiy.ja.serov.walk.hash;

import java.security.NoSuchAlgorithmException;

public abstract class StringHashBuilder implements HashBuilder<String> {

    public static String DEFAULT_ALGORITHM = "sha-256";
    protected final String hashFormat;

    protected StringHashBuilder(final String hashFormat) {
        this.hashFormat = hashFormat;
    }

    public static StringHashBuilder of(final String hashAlgorithm) throws NoSuchAlgorithmException {
        final String algorithm = hashAlgorithm.toLowerCase();

        // magic number
        return switch (algorithm) {
            case "jenkins" -> new JenkinsHash();
            case "sha-256" -> new DigestHash(algorithm, 16);
            case "md5" -> new DigestHash(algorithm, 32);
            default -> throw new NoSuchAlgorithmException("Unknown algorithm: " + algorithm);
        };
    }

    protected String getZeroHash() {
        return getHashFromInt(0);
    }

    protected String getHashFromInt(final int hash) {
        return hashFormat.formatted(hash);
    }
}
