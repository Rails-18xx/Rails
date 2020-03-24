package net.sf.rails.util;

import java.io.File;
import java.text.SimpleDateFormat;

/** Class to wrap all game file saving/reading
 *
 */
public class GamePersistence {

    protected String saveDirectory;
    protected String savePattern;
    protected String saveExtension;
    protected String savePrefix;
    protected String saveSuffixSpec = "";
    protected String saveSuffix = "";
    protected String providedName = null;
    protected SimpleDateFormat saveDateTimeFormat;
    protected File lastFile, lastDirectory;




}
