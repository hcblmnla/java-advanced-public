package info.kgeorgiy.ja.serov.implementor.output;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A wrapper over {@link BufferedWriter} writing Java code.
 * <p>
 * Writing to the file takes place immediately, if error occurred, part of the file can be saved.
 *
 * @author alnmlbch
 */
public class JavaCodeWriter implements Closeable {

    /** The main writer of the code. */
    private final BufferedWriter writer;

    /** Class token of the code carrier. */
    private final Class<?> token;

    /** Generated class name. */
    private final String name;

    /**
     * Creates a new {@link JavaCodeWriter} instance.
     *
     * @param path  writer path
     * @param token class token
     * @param name  class name
     * @throws IOException if the writer cannot be created
     * @see Files#newBufferedWriter(Path, Charset, OpenOption...)
     */
    public JavaCodeWriter(final Path path, final Class<?> token, final String name) throws IOException {
        this.writer = Files.newBufferedWriter(path);
        this.token = token;
        this.name = name;
    }

    /**
     * The main method of the class that writes the full generated file.
     * <p>
     * Performs all Java keywords like {@code package}, {@code public class}, etc.
     * Formatting is equivalent <a href="https://www.jetbrains.com/idea/">IntelliJ IDEA</a> formatting.
     *
     * @throws IOException if an output error occurs
     * @see Class#getMethods()
     */
    public void writeClass() throws IOException {
        final String packageName = token.getPackageName();
        if (!packageName.isEmpty()) {
            writeln("package %s;", packageName);
            writeln();
        }
        writeln("public class %s implements %s {", name, token.getCanonicalName());
        for (final Method method : token.getMethods()) {
            writeMethod(method);
        }
        writeln("}");
    }

    /**
     * Generates a single method implementation.
     * <p>
     * If the method is not abstract, no generation occurs, and a message is printed.
     *
     * @param method method token
     * @throws IOException if an output error occurs
     */
    private void writeMethod(final Method method) throws IOException {
        if (!Modifier.isAbstract(method.getModifiers())) {
            System.err.println("Method " + method.getName() + " is not abstract");
            return;
        }

        final String type = method.getReturnType().getCanonicalName();
        final String methodName = method.getName();

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final String parameters = IntStream.range(1, parameterTypes.length + 1)
            .mapToObj(i -> "final %s var%d".formatted(parameterTypes[i - 1].getCanonicalName(), i))
            .collect(Collectors.joining(", "));

        final Class<?>[] exceptionTypes = method.getExceptionTypes();

        writeln();
        writeln("\t@Override");

        if (exceptionTypes.length > 0) {
            writeln("\tpublic %s %s(%s) throws %s {",
                type,
                methodName,
                parameters,
                Arrays.stream(exceptionTypes)
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(", "))
            );
        } else {
            writeln("\tpublic %s %s(%s) {", type, methodName, parameters);
        }

        if (method.getReturnType() != void.class) {
            writeln("\t\treturn %s;", defaultValue(method.getReturnType()));
        }
        writeln("\t}");
    }

    /**
     * Returns the default value for the specified type as a string.
     * <p>
     * For primitive types, returns their default values (e.g., {@code false} for {@code boolean},
     * {@code 0} for numeric types). For reference types, returns {@code null}.
     *
     * @param type the type (class) token
     * @return the default value of the given type as a string
     * @see Class#isPrimitive()
     */
    private String defaultValue(final Class<?> type) {
        if (type == boolean.class) {
            return "false";
        }
        return type.isPrimitive()
            ? "(%s) 0".formatted(type.getSimpleName())
            : "null";
    }

    /**
     * Helper method which writes formatted string with {@link System#lineSeparator()}.
     *
     * @param line string template
     * @param args {@code line} arguments
     * @throws IOException if an output error occurs
     */
    private void writeln(final String line, final Object... args) throws IOException {
        writer.write(line.formatted(args));
        writeln();
    }

    /**
     * Helper method which just move pointer on next line.
     * <p>
     * This is equivalent to write {@link System#lineSeparator()}.
     *
     * @throws IOException if an output error occurs
     */
    private void writeln() throws IOException {
        writer.newLine();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
