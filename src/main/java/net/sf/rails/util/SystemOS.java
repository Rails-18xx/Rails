package net.sf.rails.util;

import java.io.File;

/**
 * Utility functions that provide support for different OS
 * specific needs
 */

public enum SystemOS {
    WINDOWS, MAC, UNIX;
    
    public static SystemOS get() {
        if (System.getProperty("os.name").toUpperCase().startsWith(WINDOWS.name())) {
            return WINDOWS;
        } else if (System.getProperty("os.name").toUpperCase().startsWith(MAC.name())) {
            return MAC;
        } else {
            return UNIX;
        }
    }

    /**
     * @return system specific folder that stores application data folders
     */
    public File getAppDataDir() {
        String pathName;
        if (this == WINDOWS) {
            // first tries to locate the application data folder
            pathName = System.getenv("APPDATA");
            if (pathName == null) {
                pathName = System.getenv("USERPROFILE");
            }
        } else if (this == MAC) {
            pathName = System.getProperty("user.home") + "/Library/Preferences";
        } else {
            pathName = System.getProperty("user.home");
        }
        File folder = new File(pathName);
        return folder;
    }
    
   /** Returns the folder that contains all rails specific user data.
     * Rerouted to save in a visible 'rails_data' folder within the project execution directory.
     * @param create set to true creates the folder if it does not exist 
     * @return rails specific configuration folder     
     */
    public File getConfigurationFolder(boolean create) {
        // Force the engine to use the visible project root instead of hidden OS directories
        String projectRoot = System.getProperty("user.dir");
        File folder = new File(projectRoot, "rails_data");
        
        if (!folder.exists() && create) {
            createFolder(folder);
        }
        
        if (folder.exists() && folder.isDirectory()) {
            return folder;
        } else {
            return null;
        }
    }
    
    /**
     * Returns a sub-folder inside the Rails configuration folder
     * Returns null if the operations fails 
     * @param subFolder the folder inside
     * @param create set to true creates the subFolder and/or
     * configFolder if it does not exist 
     * @return rails specific configuration folder 
     */
    public File getConfigurationFolder(String subFolder, boolean create) {
        File railsFolder = getConfigurationFolder(create);
        
        if (railsFolder == null) return null;
        
        // locate subFolder
        return locateFolder(railsFolder, subFolder, create);
    }

    private File locateFolder(File folder, String sub, boolean create) {
        File subFolder = new File(folder, sub);
        
        if (!subFolder.exists() && create) {
            createFolder(subFolder);
        }
        
        if (subFolder.exists() && subFolder.isDirectory()) {
            return subFolder;
        } else {
            return null;
        }
    }
    
    private boolean createFolder(File folder) {
        if (folder.exists()) {
            if (folder.isDirectory()) {
                return true;
            } else {
                return false;
            }
        } else {
            try {
                return folder.mkdir();
            } catch (Exception e) {
                return false;
            }
        }
    }
}
