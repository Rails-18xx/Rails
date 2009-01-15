/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/ResourceLoader.java,v 1.5 2009/01/15 20:53:28 evos Exp $*/
package rails.util;

import java.awt.Font;
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.*;

import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.log4j.Logger;

/**
 * Class ResourceLoader is an utility class to load a resource from a filename
 * and a list of directory.
 *
 * @version $Id: ResourceLoader.java,v 1.5 2009/01/15 20:53:28 evos Exp $
 * @author Romain Dolbeau
 * @author David Ripton
 */

public final class ResourceLoader {

    /**
     * Class ColossusClassLoader allows for class loading outside the CLASSPATH,
     * i.e. from the various variant directories.
     *
     * @version $Id: ResourceLoader.java,v 1.5 2009/01/15 20:53:28 evos Exp $
     * @author Romain Dolbeau
     */
    private static class RailsClassLoader extends ClassLoader {
        List<String> directories = null;

        protected static Logger log =
                Logger.getLogger(RailsClassLoader.class.getPackage().getName());

        RailsClassLoader(ClassLoader parent) {
            super(parent);
        }

        RailsClassLoader() {
            super();
        }

        @Override
        public Class<?> findClass(String className)
                throws ClassNotFoundException {
            try {
                int index = className.lastIndexOf(".");
                String shortClassName = className.substring(index + 1);
                if (index == -1) {
                    log.error("Loading of class \"" + className
                              + "\" failed (no dot in class name)");
                    return null;
                }
                InputStream classDataIS =
                        getInputStream(shortClassName + ".class", directories);
                if (classDataIS == null) {
                    log.error("Couldn't find the class file anywhere ! ("
                              + shortClassName + ".class)");
                    throw new FileNotFoundException("missing " + shortClassName
                                                    + ".class");
                }
                byte[] classDataBytes = new byte[classDataIS.available()];
                classDataIS.read(classDataBytes);
                return defineClass(className, classDataBytes, 0,
                        classDataBytes.length);
            } catch (Exception e) {
                return super.findClass(className);
            }
        }

        void setDirectories(List<String> d) {
            directories = d;
        }
    }

    public static final String keyContentType = "ResourceLoaderContentType";
    public static final String defaultFontName = "Lucida Sans Bold";
    public static final int defaultFontStyle = Font.PLAIN;
    public static final int defaultFontSize = 12;
    public static final Font defaultFont =
            new Font(defaultFontName, defaultFontStyle, defaultFontSize);

    // File.separator does not work in jar files, except in Unix.
    // A hardcoded '/' works in Unix, Windows, MacOS X, and jar files.
    private static final String pathSeparator = "/";
    private static final ClassLoader baseCL =
            rails.util.ResourceLoader.class.getClassLoader();
    private static final RailsClassLoader cl = new RailsClassLoader(baseCL);

    private static final Map<String, Object> fileCache =
            Collections.synchronizedMap(new HashMap<String, Object>());

    private final static String sep = "~";

    protected static Logger log =
            Logger.getLogger(ResourceLoader.class.getPackage().getName());

    private static String server = null;
    private static int serverPort = 0;

    public static void setDataServer(String server, int port) {
        ResourceLoader.server = server;
        ResourceLoader.serverPort = port;
    }

    /**
     * Give the String to mark directories.
     *
     * @return The String to mark directories.
     */
    public static String getPathSeparator() {
        return pathSeparator;
    }

    /** empty the cache so that all files have to be reloaded */
    public synchronized static void purgeFileCache() {
        log.debug("Purging File Cache.");
        fileCache.clear();
    }

    /**
     * Return the first InputStream from file of name filename in the list of
     * directories, tell the getInputStream not to complain if not found.
     *
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStreamIgnoreFail(String filename,
            List<String> directories) {
        return getInputStream(filename, directories, server != null, false,
                true);
    }

    /**
     * Return the first InputStream from file of name filename in the list of
     * directories.
     *
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStream(String filename, List<String> directories) {
        return getInputStream(filename, directories, server != null, false,
                false);
    }

    /**
     * Return the first InputStream from file of name filename in the list of
     * directories.
     *
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @param remote Ask the server for the stream.
     * @param cachedOnly Only look in the cache file, do not try to load the
     * file from permanent storage.
     * @param ignoreFail (=don't complain) if file not found
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStream(String filename, List<String> directories,
            boolean remote, boolean cachedOnly, boolean ignoreFail) {
        String mapKey = getMapKey(filename, directories);
        Object cached = fileCache.get(mapKey);
        byte[] data = null;

        if ((cached == null) && cachedOnly) {
            if (!ignoreFail) {
                log.warn("Requested file " + filename
                         + " is requested cached-only but is not is cache.");
            }
            return null;
        }

        if ((cached == null) && ((!remote) || (server == null))) {
            synchronized (fileCache) {
                InputStream stream = null;
                java.util.Iterator<String> it = directories.iterator();
                while (it.hasNext() && (stream == null)) {
                    Object o = it.next();
                    if (o instanceof String) {
                        String path = (String) o;
                        String fullPath =
                                path + pathSeparator + fixFilename(filename);

                        log.debug("Trying to locate InputStream: " + path
                                  + pathSeparator + filename);
                        try {
                            File tempFile = new File(fullPath);
                            stream = new FileInputStream(tempFile);
                        } catch (Exception e) {
                            stream = cl.getResourceAsStream(fullPath);
                        }
                    }
                }
                if (stream == null) {
                    if (!remote && ignoreFail) {
                        // If someone locally requests it as ignoreFail,
                        // let's assume a remote requester later sees it the
                        // same way.
                        // Right now, the remote-requesting is not able to
                        // submit the "ignore-fail" property...
                        // @TODO: submit that properly?
                        // fileCacheIgnoreFail.put(mapKey, new Boolean(true));
                    }
                    if (!ignoreFail) {
                        log.warn("getInputStream:: "
                                 + " Couldn't get InputStream for file "
                                 + filename + " in " + directories
                                 + (cachedOnly ? " (cached only)" : ""));
                        // @TODO this sounds more serious than just a warning in
                        // the logs
                        // Anyway now at least MarkersLoader does not complain
                        // any more...
                    }
                } else {
                    data = getBytesFromInputStream(stream);
                    fileCache.put(mapKey, data);
                }
            }
        } else {
            synchronized (fileCache) {
                if (cached != null) {
                    data = (byte[]) cached;
                } else {
                    try {
                        Socket fileSocket = new Socket(server, serverPort);
                        InputStream is = fileSocket.getInputStream();

                        if (is == null) {
                            log.warn("getInputStream:: "
                                     + " Couldn't get InputStream from socket"
                                     + " for file " + filename + " in "
                                     + directories
                                     + (cachedOnly ? " (cached only)" : ""));
                            // @TODO this sounds more serious than just a
                            // warning in the logs
                        } else {
                            PrintWriter out =
                                    new PrintWriter(
                                            fileSocket.getOutputStream(), true);

                            if (ignoreFail) {
                                // Not in this version yet (05/2007).
                                // New clients could not talk with old server.
                                // Take this into full use somewhat later.
                                // out.print(
                                // Constants.fileServerIgnoreFailSignal + sep);
                            }
                            out.print(filename);
                            java.util.Iterator<String> it = directories.iterator();
                            while (it.hasNext()) {
                                out.print(sep + it.next());
                            }
                            out.println();
                            data = getBytesFromInputStream(is);
                            if (data != null && data.length == 0 && !ignoreFail) {
                                log.warn("Got empty contents for file "
                                         + filename + " directories "
                                         + directories.toString());
                            }
                            fileSocket.close();
                            fileCache.put(mapKey, data);
                        }
                    } catch (Exception e) {
                        log.error("ResourceLoader::getInputStream() : " + e);
                    }
                }

            }
        }
        return (data == null ? null : getInputStreamFromBytes(data));
    }

    /**
     * Return the content of the specified file as an array of byte.
     *
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @param cachedOnly Only look in the cache file, do not try to load the
     * file from permanent storage.
     * @return An array of byte representing the content of the file, or null if
     * it fails.
     */
    public static byte[] getBytesFromFile(String filename, List<String> directories,
            boolean cachedOnly, boolean ignoreFail) {
        InputStream is =
                getInputStream(filename, directories, server != null,
                        cachedOnly, ignoreFail);
        if (is == null) {
            // right now only FileServerThread is using this method at all.
            if (!ignoreFail) {
                log.warn("getBytesFromFile:: "
                         + " Couldn't get InputStream for file " + filename
                         + " in " + directories
                         + (cachedOnly ? " (cached only)" : ""));
            }
            return null;
        }
        return getBytesFromInputStream(is);
    }

    /**
     * Return the content of the specified InputStream as an array of byte.
     *
     * @param InputStream The InputStream to use.
     * @return An array of byte representing the content of the InputStream, or
     * null if it fails.
     */
    private static byte[] getBytesFromInputStream(InputStream is) {
        byte[] all = new byte[0];

        try {
            byte[] data = new byte[1024 * 64];
            int r = is.read(data);
            while (r > 0) {
                byte[] temp = new byte[all.length + r];
                for (int i = 0; i < all.length; i++) {
                    temp[i] = all[i];
                }
                for (int i = 0; i < r; i++) {
                    temp[i + all.length] = data[i];
                }
                all = temp;
                r = is.read(data);
            }
        } catch (Exception e) {
            log.error("Can't Stringify stream " + is + " (" + e + ")");
        }
        return all;
    }

    /**
     * Return the content of the specified byte array as an InputStream.
     *
     * @param data The byte array to convert.
     * @return An InputStream whose content is the data byte array.
     */
    private static InputStream getInputStreamFromBytes(byte[] data) {
        if (data == null) {
            log.warn("getInputStreamFromBytes:: "
                     + " Can't create InputStream from null byte array");
            return null;
        }
        return new ByteArrayInputStream(data);
    }

    /**
     * Return the first OutputStream from file of name filename in the list of
     * directories.
     *
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The OutputStream, or null if it was not found.
     */
    public static OutputStream getOutputStream(String filename, List<String> directories) {
        OutputStream stream = null;
        java.util.Iterator<String> it = directories.iterator();
        while (it.hasNext() && (stream == null)) {
            Object o = it.next();
            if (o instanceof String) {
                String path = (String) o;
                String fullPath = path + pathSeparator + fixFilename(filename);
                try {
                    stream = new FileOutputStream(fullPath);
                } catch (Exception e) {
                    log.debug("getOutputStream:: "
                              + " Couldn't get OutputStream for file "
                              + filename + " in " + directories + "("
                              + e.getMessage() + ")");
                }
            }
        }
        return (stream);
    }

    /**
     * Return the first Document from file of name filename in the list of
     * directories. It also add a property of key keyContentType and of type
     * String describing the content type of the Document. This can currently
     * load HTML and pure text.
     *
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The Document, or null if it was not found.
     */
    public static Document getDocument(String filename, List<String> directories) {
        InputStream htmlIS =
                getInputStreamIgnoreFail(filename + ".html", directories);
        if (htmlIS != null) {
            try {
                HTMLEditorKit htedk = new HTMLEditorKit();
                HTMLDocument htdoc = new HTMLDocument(htedk.getStyleSheet());
                htdoc.putProperty(keyContentType, "text/html");
                htedk.read(htmlIS, htdoc, 0);
                return htdoc;
            } catch (Exception e) {
                log.error("html document exists, but cannot be loaded ("
                          + filename + "): " + e);
            }
            return null;
        }
        InputStream textIS =
                getInputStreamIgnoreFail(filename + ".txt", directories);
        if (textIS == null) {
            textIS = getInputStreamIgnoreFail(filename, directories);
        }
        if (textIS != null) {
            try {
                // Must be a StyledDocument not a PlainDocument for
                // JEditorPane.setDocument()
                StyledDocument txtdoc = new DefaultStyledDocument();
                char[] buffer = new char[128];
                InputStreamReader textISR = new InputStreamReader(textIS);
                int read = 0;
                int offset = 0;
                while (read != -1) {
                    read = textISR.read(buffer, 0, 128);
                    if (read != -1) {
                        txtdoc.insertString(offset,
                                new String(buffer, 0, read), null);
                        offset += read;
                    }
                }
                txtdoc.putProperty(keyContentType, "text/plain");
                return txtdoc;
            } catch (Exception e) {
                log.error("text document exists, but cannot be loaded ("
                          + filename + "): " + e);
            }
            return null;
        }
        log.error("No document for basename " + filename + " found "
                  + "(neither .html, .txt nor without extention)!");
        return null;
    }

    /**
     * Return the key to use in the image and file caches.
     *
     * @param filename Name of the file.
     * @param directories List of directories.
     * @return A String to use as a key when storing/loading in a cache the
     * specified file from the specified list of directories.
     */
    private static String getMapKey(String filename, List<String> directories) {
        String[] filenames = new String[1];
        filenames[0] = filename;
        return getMapKey(filenames, directories);
    }

    /**
     * Return the key to use in the image cache.
     *
     * @param filenames Array of name of files.
     * @param directories List of directories.
     * @return A String to use as a key when storing/loading in a cache the
     * specified array of name of files from the specified list of directories.
     */
    private static String getMapKey(String[] filenames, List<String> directories) {
        StringBuffer buf = new StringBuffer(filenames[0]);
        for (int i = 1; i < filenames.length; i++) {
            buf.append(",");
            buf.append(filenames[i]);
        }
        Iterator<String> it = directories.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof String) {
                buf.append(",");
                buf.append(o);
            }
        }
        return buf.toString();
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

    /**
     * Create an instance of the class whose name is in parameter.
     *
     * @param className The name of the class to use.
     * @param directories List of directories to search (in order).
     * @return A new object, instance from the given class.
     */
    public static Object getNewObject(String className, List<String> directories) {
        return getNewObject(className, directories, null);
    }

    /**
     * Create an instance of the class whose name is in parameter, using
     * parameters.
     *
     * If no parameters are given, the default constructor is used.
     *
     * @TODO this is full of catch(Exception) blocks, which all return null.
     * Esp. returning null seems a rather bad idea, since it will most likely
     * turn out to be NPEs somewhere later.
     *
     * @param className The name of the class to use, must not be null.
     * @param directories List of directories to search (in order), must not be
     * null.
     * @param parameter Array of parameters to pass to the constructor, can be
     * null.
     * @return A new object, instance from the given class or null if
     * instantiation failed.
     */
    public static Object getNewObject(String className, List<String> directories,
            Object[] parameter) {
        Class<?> theClass = null;
        cl.setDirectories(directories);
        try {
            theClass = cl.loadClass(className);
        } catch (Exception e) {
            log.error("Loading of class \"" + className + "\" failed (" + e
                      + ")");
            return null;
        }
        if (parameter != null) {
            Class<?>[] paramClasses = new Class[parameter.length];
            for (int i = 0; i < parameter.length; i++) {
                paramClasses[i] = parameter[i].getClass();
            }
            try {
                Constructor<?> c = theClass.getConstructor(paramClasses);
                return c.newInstance(parameter);
            } catch (Exception e) {
                log.error("Loading or instantiating class' constructor for \""
                          + className + "\" failed (" + e + ")");
                return null;
            }
        } else {
            try {
                return theClass.newInstance();
            } catch (Exception e) {
                log.error("Instantiating \"" + className + "\" failed (" + e
                          + ")");
                return null;
            }
        }
    }

    /**
     * Force adding the given data as belonging to the given filename in the
     * file cache.
     *
     * @param filename Name of the Image file to add.
     * @param directories List of directories to search (in order).
     * @param data File content to add.
     */
    public static void putIntoFileCache(String filename, List<String> directories,
            byte[] data) {
        String mapKey = getMapKey(filename, directories);
        fileCache.put(mapKey, data);
    }

    /**
     * Force adding the given data as belonging to the given key in the file
     * cache.
     *
     * @see #getMapKey(String, List)
     * @see #getMapKey(String[], List)
     * @param mapKey Key to use in the cache.
     * @param data File content to add.
     */
    public static void putIntoFileCache(String mapKey, byte[] data) {
        fileCache.put(mapKey, data);
    }
}
