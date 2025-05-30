package info.kgeorgiy.ja.serov.implementor.output;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A safe wrapper over temporary {@link File} directory.
 * <p>
 * Using with {@code try-with-resources} will automatically clear files.
 *
 * @author alnmlbch
 */
public class TemporaryDirectory implements Closeable {

    /**
     * Default version of {@link FileVisitor} which safely deletes all directories.
     */
    private static final FileVisitor<Path> DELETE = new SimpleFileVisitor<>() {

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
            throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
            throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /** Inner directory. */
    private final Path dir;

    /**
     * Creates a new {@link TemporaryDirectory} instance (temporary directory).
     *
     * @param root path of the directory
     * @throws IOException if directory cannot be created
     */
    public TemporaryDirectory(final Path root) throws IOException {
        this.dir = Files.createDirectory(root);
    }

    /**
     * Inner directory getter.
     *
     * @return inner directory
     */
    public Path dir() {
        return dir;
    }

    @Override
    public void close() throws IOException {
        Files.walkFileTree(dir, DELETE);
    }
}
