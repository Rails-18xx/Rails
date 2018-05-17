using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Common
{
    public sealed class ResourceLoader
    {
        private static Logger<ResourceLoader> log = new Logger<ResourceLoader>();

    public const string SEPARATOR = "/";

        // #FIXME file handling
    //public static InputStream getInputStream(String filename, String directory)
    //    {
    //        String fullPath = directory + SEPARATOR + fixFilename(filename);
    //        log.debug("Locate fullPath (updated) =" + fullPath);
    //        return ResourceLoader.class.getClassLoader().getResourceAsStream(fullPath);
    //}

    /**
     * Fix a filename by replacing space with underscore.
     *
     * @param filename Filename to fix.
     * @return The fixed filename.
     */
    private static string FixFilename(string filename)
    {
        return filename.Replace(' ', '_');
    }
}
}
