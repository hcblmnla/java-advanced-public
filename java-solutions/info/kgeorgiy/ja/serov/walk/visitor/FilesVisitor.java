package info.kgeorgiy.ja.serov.walk.visitor;

import info.kgeorgiy.ja.serov.walk.hash.HashBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FilesVisitor extends DirectoriesVisitor {

    public FilesVisitor(final HashBuilder<String> hash, final BufferedWriter writer) {
        super(hash, writer);
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {
        visitFileFailedImpl(dir.toString());
        return FileVisitResult.SKIP_SUBTREE;
    }
}
