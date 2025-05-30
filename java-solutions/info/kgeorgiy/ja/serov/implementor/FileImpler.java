package info.kgeorgiy.ja.serov.implementor;

import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * An extended version of {@link JarImpler} that provides default file constants for code generation.
 * <p>
 * This interface defines common constants used in generating:
 * <ul>
 *     <li>Java source files: {@code .java}</li>
 *     <li>Jar archives: {@code .jar}</li>
 * </ul>
 *
 * @author alnmlbch
 * @see JarImpler
 */
public interface FileImpler extends JarImpler {

    /** Temporary directory path. */
    Path GENERATED = Path.of("generated");

    /** Java file extension. */
    String JAVA_EXTENSION = ".java";

    /** Class file extension. */
    String CLASS_EXTENSION = ".class";

    /** Java package char separator. */
    char PACKAGE_SEPARATOR = '.';

    /** Jar files char separator. */
    char JAR_SEPARATOR = '/';

    /**
     * Puts a label indicating that the class is inherited.
     *
     * @param interfaceName interface provider name
     * @return inherited class name
     */
    default String markInheritor(final String interfaceName) {
        return interfaceName + "Impl";
    }

    /**
     * Prepares default version of {@code MANIFEST.MF}.
     * <p>
     * The method can be overridden for adding additional information.
     *
     * @return manifest instance
     */
    default Manifest prepareManifest() {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }
}
