package info.kgeorgiy.ja.serov.walk.visitor;

import info.kgeorgiy.ja.serov.walk.hash.HashBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoriesVisitor extends SimpleFileVisitor<Path> {

    private static final int BUFFER_SIZE = 1024;

    private final HashBuilder<String> hash;
    private final BufferedWriter writer;
    private final byte[] buffer;

    public DirectoriesVisitor(final HashBuilder<String> hash, final BufferedWriter writer) {
        this(hash, writer, BUFFER_SIZE);
    }

    private DirectoriesVisitor(
        final HashBuilder<String> hash,
        final BufferedWriter writer,
        final int bufferSize
    ) {
        this.hash = hash;
        this.writer = writer;
        this.buffer = new byte[bufferSize];
    }

    public void walkFileTree(final String stringPath) throws IOException {
        try {
            final Path path = Path.of(stringPath);
            Files.walkFileTree(path, this);
        } catch (final IOException | InvalidPathException e) {
            visitFileFailedImpl(stringPath);
        }
    }

    private void visitFileImpl(final Path file) throws IOException {
        try (final InputStream stream = Files.newInputStream(file)) {
            int read;
            while ((read = stream.read(buffer)) != -1) {
                hash.update(read, buffer);
            }
            writeln(file.toString(), hash.getHash());
        }
    }

    protected void visitFileFailedImpl(final String name) throws IOException {
        writeln(name, hash.getEmptyHash());
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        visitFileImpl(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        visitFileFailedImpl(file.toString());
        return FileVisitResult.CONTINUE;
    }

    private void writeln(final String name, final String hash) throws IOException {
        writer.write("%s %s".formatted(hash, name));
        writer.newLine();
    }
}
