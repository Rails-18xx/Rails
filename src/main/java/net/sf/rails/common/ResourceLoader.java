package net.sf.rails.common;

import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public final class ResourceLoader {

    public static final String SEPARATOR = "/";

    public static InputStream getInputStream(String filename, String directory) {
        String fullPath = directory + SEPARATOR + fixFilename(filename);
        log.trace("Locate fullPath (updated) = {}", fullPath);
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
