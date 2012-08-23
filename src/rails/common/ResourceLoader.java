package rails.common;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResourceLoader {
    private static final Logger log = LoggerFactory.getLogger(ResourceLoader.class);

    private static final String PATH_SEPARATOR = "/";

    public static InputStream getInputStream(String filename, String directory) {
        String fullPath = directory + PATH_SEPARATOR + fixFilename(filename);
        log.debug("Locate fullPath=" + fullPath);
        return ResourceLoader.class.getClassLoader().getResourceAsStream(fullPath);
    }

    /**
     * Fix a filename by replacing space with underscore.
     *
     * @param filename Filename to fix.
     * @return The fixed filename.
     */
    private static String fixFilename(String filename) {
        return filename.replace(' ', '_');
    }
}
