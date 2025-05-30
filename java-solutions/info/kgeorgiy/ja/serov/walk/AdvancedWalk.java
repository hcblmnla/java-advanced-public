package info.kgeorgiy.ja.serov.walk;

import info.kgeorgiy.ja.serov.walk.hash.HashBuilder;
import info.kgeorgiy.ja.serov.walk.hash.StringHashBuilder;
import info.kgeorgiy.ja.serov.walk.visitor.DirectoriesVisitor;
import info.kgeorgiy.ja.serov.walk.visitor.FilesVisitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public enum AdvancedWalk {
    FILES,
    DIRECTORIES;

    public static void walk(final String[] args, final AdvancedWalk mode) {
        if (args == null) {
            error("Walk was called with null");
            return;
        }
        if (args.length != 2 && args.length != 3) {
            error("Invalid number of arguments: expected 2 or 3, but actual " + args.length);
            error("Usage: java Walk <input> <output> [algorithm]");
            return;
        }
        if (args[0] == null || args[1] == null || args.length == 3 && args[2] == null) {
            error("Walk was called with null argument");
            return;
        }

        final String inputPath = args[0];
        final String outputPath = args[1];
        final String algorithm = args.length == 3
            ? args[2]
            : StringHashBuilder.DEFAULT_ALGORITHM;

        try (final BufferedReader reader = Files.newBufferedReader(
            Path.of(inputPath),
            StandardCharsets.UTF_8
        )) {
            try (final BufferedWriter writer = Files.newBufferedWriter(
                getOutputPath(outputPath),
                StandardCharsets.UTF_8
            )) {
                final HashBuilder<String> hash = StringHashBuilder.of(algorithm);
                final DirectoriesVisitor visitor = switch (mode) {
                    case FILES -> new FilesVisitor(hash, writer);
                    case DIRECTORIES -> new DirectoriesVisitor(hash, writer);
                };
                String line;
                while ((line = reader.readLine()) != null) {
                    visitor.walkFileTree(line);
                }
            } catch (final InvalidPathException e) {
                error("Invalid output path: " + outputPath);
            } catch (final NoSuchAlgorithmException e) {
                error("Algorithm not found: " + algorithm);
            }
        } catch (final InvalidPathException e) {
            error("Invalid input path: " + inputPath);
        } catch (final SecurityException e) {
            error("Security error occurred: " + e.getMessage());
        } catch (final IOException e) {
            error("Reading/writing error occurred: " + e);
        }
    }

    private static Path getOutputPath(final String stringPath) throws IOException {
        final Path output = Path.of(stringPath);
        final Path outputParent = output.getParent();
        if (outputParent != null && Files.notExists(outputParent)) {
            Files.createDirectories(outputParent);
        }
        return output;
    }

    private static void error(final String message) {
        System.err.println(message);
    }
}
