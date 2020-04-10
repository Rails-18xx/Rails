package net.sf.rails.util;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.ConfigProfile;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.state.Item;


public final class Util {

    private static final Logger log = LoggerFactory.getLogger(Util.class);

    /**
     * No-args private constructor, to prevent (meaningless) construction of one
     * of these.
     */
    private Util() {}

    public static boolean hasValue(String s) {
        return s != null && !s.equals("");
    }

    public static String valueWithDefault(String s, String defaultValue) {
        if (hasValue(s)) {
            return s;
        } else {
            return defaultValue;
        }
    }

    public static String appendWithDelimiter(String s1, String s2,
            String delimiter) {
        StringBuilder b = new StringBuilder(s1 != null ? s1 : "");
        if (b.length() > 0) b.append(delimiter);
        b.append(s2);
        return b.toString();
    }

    public static String joinWithDelimiter (String[] sa, String delimiter) {
        if (sa == null || sa.length == 0) return "";
        StringBuilder b = new StringBuilder();
        for (String s : sa) {
            if (Util.hasValue(delimiter) && b.length() > 0) b.append(delimiter);
            b.append(s);
        }
        return b.toString();
    }

    public static String joinNamesWithDelimiter (Iterable<? extends Item> items, String delimiter) {
        StringBuilder b = new StringBuilder("");
        if (items != null) {
            for (Item company : items) {
                if (b.length() > 0) b.append(delimiter);
                b.append(company.getId());
            }
        }
        return b.toString();
    }


    public static String joinWithDelimiter (Iterable<Integer> sa, String delimiter) {
        StringBuilder b = new StringBuilder();
        for (int s : sa) {
            if (b.length() > 0) b.append(delimiter);
            b.append(s);
        }
        return b.toString();
    }

    public static int parseInt(String value) throws ConfigurationException {

        if (!hasValue(value)) return 0;

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid integer value: " + value,
                    e);
        }
    }

    public static boolean bitSet(int value, int bitmask) {

        return (value & bitmask) > 0;
    }

    public static int setBit(int value, int bitmask, boolean set) {

        if (set) {
            return bitmask | value;
        } else {
            System.out.println("Reset bit " + value + ": from " + bitmask
                    + " to " + (bitmask & ~value));
            return bitmask & ~value;
        }
    }


    /**
     * Convert java string to html string
     * Transformations:
     * - Converts \n to <br>
     */
    public static String convertToHtml(String javaString) {
        return javaString.replace("\n", "<br>");
    }

    /**
     * Parse a boolean value for Rails
     * @param value string (allowed values for true: standard Boolean.parseBoolean and yes (after conversion to lowercase)
     * @return parsed value
     */
    public static boolean parseBoolean(String s) {
        if (s.toLowerCase().equals("yes")) {
            return true;
        }
        return Boolean.parseBoolean(s);
    }

    /**
     * Parse a colour definition string.
     * Currently supported formats:
     *   "RRGGBB" - each character being a hexadecimal digit
     *   "r,g,b"  - each letter representing an integer 0..255
     * @param s
     * @return
     */
    public static Color parseColour (String s) throws ConfigurationException{
        Color c = null;
        if (!Util.hasValue(s)) {
        } else if (s.indexOf(',') == -1) {
            // Assume hexadecimal RRGGBB
            try {
                c = new Color (Integer.parseInt(s, 16));
            } catch (NumberFormatException e) {
                log.error("Invalid hex RGB colour: {}", s, e);
                throw new ConfigurationException (e);
            }
        } else {
            // Assume decimal r,g,b
            try {
                String[] parts = s.split(",");
                c = new Color (Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                log.error("Invalid nummeric RGB colour: {}", s, e);
                throw new ConfigurationException (e);
            }
        }
        return c;
    }




    /**
     * Is a colour dark? (to check if FG colour needs be reversed)
     */
    public static boolean isDark(Color c) {
        if (c == null) return false;
        return Math.sqrt(0.241*c.getRed()*c.getRed()
                + 0.691*c.getBlue()*c.getBlue()
                + 0.068*c.getGreen()*c.getGreen()) < 128;
        // Copied this formula from
        // http://www.nbdtech.com/blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
    }

    public static Logger getLogger () {
        return log;
    }

    public static String lowerCaseFirst (String text) {
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }

    /**
     * Open an input stream from a file, which may exist as a physical file or
     * in a JAR file. The name must be valid for both options.
     *
     * @author Erik Vos
     */
    public static InputStream getStreamForFile(String fileName)
    throws IOException {

        File file = new File(fileName);
        if (file.exists()) {
            return new FileInputStream(file);
        } else {
            return null;
        }
    }

    public static boolean loadProperties(Properties properties, File file) {
        try {
            log.info("Loading properties from file {}", file);
            InputStream inFile = new FileInputStream(file);
            properties.load(inFile);
        } catch (Exception e) {
            log.error("{} whilst loading properties file {}", e, file, e);
            return false;
        }
        return true;
    }

    public static boolean loadPropertiesFromResource(Properties properties, String resourcePath) {
        try {
            log.info("Loading properties from resource {}", resourcePath);
            InputStream inFile;
                inFile = ConfigProfile.class.getClassLoader().getResourceAsStream(resourcePath);
            properties.load(inFile);
        } catch (Exception e) {
            log.error("Error while loading properties file from resource at {}", resourcePath, e);
            return false;
        }
        return true;
    }

    public static boolean storeProperties(Properties properties, File file) {
        boolean result = true;
        try {
            properties.store(new FileOutputStream(file), "Automatically generated, do not edit");
            log.info("Storing properties to file {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error while storing properties file {}", file.getAbsolutePath(), e);
            result = false;
        }
        return result;
    }
}
