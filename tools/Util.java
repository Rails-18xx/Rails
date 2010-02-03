/* $Header: /Users/blentz/rails_rcs/cvs/18xx/tools/Util.java,v 1.1 2010/02/03 20:16:38 evos Exp $*/
package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Util {
    /**
     * No-args private constructor, to prevent (meaningless) construction of one
     * of these.
     */
    private Util() {}

    public static boolean hasValue(String s) {
        return s != null && !s.equals("");
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

}
