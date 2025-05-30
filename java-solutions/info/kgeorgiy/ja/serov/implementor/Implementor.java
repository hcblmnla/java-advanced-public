package info.kgeorgiy.ja.serov.implementor;

import info.kgeorgiy.ja.serov.implementor.output.JavaCodeWriter;
import info.kgeorgiy.ja.serov.implementor.output.TemporaryDirectory;
import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Generates class implementation by {@link Class type token} of interface.
 * <p>
 * Supports generating into {@code .jar} files already compiled code.
 * Generating can be called also by {@link Implementor#main(String[]) main} method.
 *
 * @author alnmlbch
 */
public class Implementor implements FileImpler {

    /** Implementor instance. */
    private static final Impler IMPLER = new Implementor();

    /** Jar implementor instance. */
    private static final JarImpler JAR_IMPLER = new Implementor();

    /** Working directory. */
    private static final Path ROOT = Path.of("");

    /** Command line {@code jar} option. */
    private static final String JAR_OPTION = "-jar";

    /** Public default constructor. */
    public Implementor() {
    }

    /**
     * Runs {@link Implementor implementor} application.
     * <p>
     * This method is equivalent calling {@link #implement(Class, Path)}
     * or {@link #implementJar(Class, Path)} depending on the flag {@value JAR_OPTION}
     * with specified interface name and working directory.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        if (args.length != 1 && args.length != 3) {
            printUsage();
            return;
        }
        final String interfaceName;
        final Path root;
        final Impler impler;
        if (args.length == 3) {
            if (!JAR_OPTION.equals(args[0])) {
                printUsage();
                return;
            }
            interfaceName = args[1];
            final String rootString = args[2];
            try {
                root = Path.of(rootString);
            } catch (final InvalidPathException e) {
                System.err.println("Invalid path: " + rootString);
                return;
            }
            impler = JAR_IMPLER::implementJar;
        } else {
            interfaceName = args[0];
            root = ROOT;
            impler = IMPLER;
        }
        try {
            impler.implement(Class.forName(interfaceName), root);
        } catch (final ClassNotFoundException e) {
            System.err.format("Could not load interface %s%n", interfaceName);
        } catch (final ImplerException e) {
            System.err.format("Implementing error occurred: %s%n", e.getMessage());
        }
    }

    /**
     * Helper method which prints usage in {@link System#err stderr}.
     */
    private static void printUsage() {
        System.err.println("""
            Usage: java Implementor <interfaceName>
                   java Implementor -jar <interfaceName> <file.jar>
            """);
    }

    /**
     * Provides that given {@link Class type token} is not {@code private}
     * and is {@code interface}.
     *
     * @param token type token
     * @return simple name of the given token
     * @throws ImplerException if the check failed
     * @see Class#isInterface()
     * @see Modifier#isPrivate(int)
     * @see Class#getSimpleName()
     */
    private String requireNonPrivateInterface(final Class<?> token) throws ImplerException {
        if (!token.isInterface()) {
            throw ie("%s is not an interface", token.getCanonicalName());
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw ie("%s is private", token.getCanonicalName());
        }
        return token.getSimpleName();
    }

    /**
     * Replaces {@value FileImpler#PACKAGE_SEPARATOR} in package string name
     * to specified separator.
     *
     * @param packagePath  string name of the given package
     * @param newSeparator new file separator
     * @return replaced string
     */
    private String replacePackage(final String packagePath, final char newSeparator) {
        return packagePath.replace(PACKAGE_SEPARATOR, newSeparator);
    }

    /**
     * Utility which creates a Java package by the given {@link Path path}.
     *
     * @param root        source directory
     * @param packagePath string name of the package
     * @return path of the created package
     * @throws ImplerException if package cannot be created
     */
    private Path createPackage(final Path root, final String packagePath) throws ImplerException {
        try {
            final Path path = root.resolve(replacePackage(packagePath, File.separatorChar));
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
            return path;
        } catch (final InvalidPathException | IOException e) {
            throw ie(e, "Could not create package at %s", root);
        } catch (final SecurityException e) {
            throw ie(e, "No such access rights to create package at %s", root);
        }
    }

    /**
     * Produces code implementing interface specified
     * by provided {@link Class type token}.
     *
     * @param token type token
     * @param root  working directory
     * @return path of the generated code
     * @throws ImplerException if implementation error occurs
     * @see Implementor#implement(Class, Path)
     */
    private Path implementAndGetPath(final Class<?> token, final Path root) throws ImplerException {
        final String name = markInheritor(requireNonPrivateInterface(token));
        final Path path = createPackage(root, token.getPackageName()).resolve(name + JAVA_EXTENSION);
        try (final JavaCodeWriter writer = new JavaCodeWriter(path, token, name)) {
            writer.writeClass();
            return path;
        } catch (final IOException e) {
            throw ie(e, "Implementation writing error occurred: %s", e.getMessage());
        } catch (final SecurityException e) {
            throw ie(e, "No such access rights to write implementation at %s", path);
        }
    }

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        implementAndGetPath(token, root);
    }

    /**
     * Classpath getter by {@link ProtectionDomain}.
     *
     * @param token type token
     * @return {@link Path classpath} of the given token
     */
    private Path getClassPath(final Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Compiles generated by {@link Impler#implement(Class, Path)} Java class to
     * {@code .class} file.
     *
     * @param token        type token
     * @param temp         working directory
     * @param compiledName name of the generated class
     * @return path to compiled {@code .class} file
     * @throws ImplerException if implementation error occurs
     * @see Implementor#implement(Class, Path)
     */
    private Path compileAndGetPath(final Class<?> token, final Path temp, final String compiledName)
        throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw ie("Could not find java compiler, include tools.jar to classpath");
        }
        final Path path = implementAndGetPath(token, temp);
        final String[] args = {
            "-cp", getClassPath(token).toString(),
            "-encoding", StandardCharsets.UTF_8.name(),
            path.toString()
        };
        final int code = compiler.run(null, null, null, args);
        if (code != 0) {
            throw ie("Compilation of generated file failed with code %d", code);
        }
        return path.getParent().resolve(compiledName);
    }

    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        try (final TemporaryDirectory temp = new TemporaryDirectory(GENERATED)) {
            try (final JarOutputStream out = new JarOutputStream(
                new BufferedOutputStream(Files.newOutputStream(jarFile)),
                prepareManifest()
            )) {
                final String className = markInheritor(token.getSimpleName()) + CLASS_EXTENSION;
                out.putNextEntry(new ZipEntry(
                    replacePackage(token.getPackageName(), JAR_SEPARATOR) + JAR_SEPARATOR + className
                ));
                Files.copy(compileAndGetPath(token, temp.dir(), className), out);
                out.closeEntry();
            } catch (final IOException e) {
                throw ie(e, "Could not write generated jar file");
            }
        } catch (final IOException e) {
            throw ie(e, "Could not use temporary directory: %s", e.getMessage());
        }
    }

    /**
     * {@link ImplerException} builder with specified message.
     *
     * @param message string template
     * @param args    string template args
     * @return {@link ImplerException} instance
     * @see ImplerException#ImplerException(String)
     */
    private ImplerException ie(final String message, final Object... args) {
        return new ImplerException(message.formatted(args));
    }

    /**
     * {@link ImplerException} builder with specified cause and message.
     *
     * @param cause   error cause
     * @param message string template
     * @param args    string template args
     * @return {@link ImplerException} instance
     * @see ImplerException#ImplerException(String, Throwable)
     */
    private ImplerException ie(final Throwable cause, final String message, final Object... args) {
        return new ImplerException(message.formatted(args), cause);
    }
}
