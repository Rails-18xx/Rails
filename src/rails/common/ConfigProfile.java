package rails.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import rails.util.SystemOS;
import rails.util.Util;

/**
 * A profile storing configuration settings
 */

public final class ConfigProfile {
    private static final Logger log =
            LoggerFactory.getLogger(ConfigItem.class);
    
    // available profile types
    public enum Type {SYSTEM, PREDEFINED, USER};
    
    // Filename extension of profiles
    public static final String PROFILE_EXTENSION = ".rails_profile";
    private static final String PREDEFINED_EXTENSION = ".predefined";

    // Location of predefined profiles
    private static final String PREDEFINED_FOLDER = "data/profiles/";
    
    // predefined default profiles
    private static final String ROOT_PROFILE = "root";
    private static final String TEST_PROFILE = "test";
    
    // the profile selected at the start ...
    private static final String STANDARD_PROFILE = "pbem";
    // ... unless a cli option has been set
    private static final String STANDARD_CLI_OPTION ="profile";
    
    
    // file that stores the list of predefined profiles
    private static final String LIST_OF_PROFILES = "LIST_OF_PROFILES";
    
    // property key of predefined profile in user profile
    private static final String PARENT_KEY = "profile.parent";
    private static final String FINAL_KEY = "profile.final";

    // map of all profiles
    private static final Map<String, ConfigProfile> profiles = new HashMap<String, ConfigProfile>(); 
    
    // root profile
    private static ConfigProfile root;
    
    // profile type
    private final Type type;
    
    // profile name
    private final String name;

    // profile properties
    private final Properties properties = new Properties();
    
    // profile loaded
    private boolean loaded = false;
    
    // profile parent
    private ConfigProfile parent = null;

    
    static void loadRoot() {
        root = new ConfigProfile(Type.SYSTEM, ROOT_PROFILE);
        root.load();
    }
    
    static ConfigProfile loadTest() {
        ConfigProfile test =  new ConfigProfile(Type.SYSTEM, TEST_PROFILE);
        test.load();
        return test;
    }
    
    static void readPredefined() {
        Properties list = new Properties();
        String filePath = PREDEFINED_FOLDER + LIST_OF_PROFILES;
        loadProperties(list, filePath, true);
        for (String name:list.stringPropertyNames()) {
            new ConfigProfile(Type.PREDEFINED, name);
        }
    }
    
    static void readUser() {
        File userFolder = SystemOS.get().getConfigurationFolder(false);
        if (userFolder == null) return;
        FilenameFilter filter = new SuffixFileFilter(PROFILE_EXTENSION, IOCase.SYSTEM);
        for (String fileName:userFolder.list(filter)) {
            new ConfigProfile(Type.USER, FilenameUtils.getBaseName(fileName));
        }
    }
    
    static ConfigProfile getDefault() {
        String profile = System.getProperty(STANDARD_CLI_OPTION);
        if (Util.hasValue(profile) && profiles.containsKey(profile)) {
            return profiles.get(profile);
        } 
        return profiles.get(STANDARD_PROFILE);
    }
    
    static ConfigProfile getProfile(String name) {
        return profiles.get(name);
    }
    
    static Set<String> getListofProfiles() {
        return profiles.keySet();
    }
    
    private ConfigProfile(Type type, String name) {
        this.type = type;
        this.name = name;
        if (type != Type.SYSTEM) {
            profiles.put(name, this);
        }
    }
        
    public Type getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    boolean isLoaded() {
        return loaded;
    }
    
    boolean isFinal() {
        if (!loaded && type == Type.USER) return true;
        
        if (Util.hasValue(properties.getProperty(FINAL_KEY))) {
            return Util.parseBoolean(properties.getProperty(FINAL_KEY));
        }
        return false;
    }
    
    ConfigProfile setParent(ConfigProfile parent) {
        this.parent = parent;
        properties.setProperty(PARENT_KEY, parent.getName());
        return this;
    }
    
    private ConfigProfile setParent(String name) {
        return setParent(profiles.get(name));
    }

    ConfigProfile getParent() {
        return parent;
    }
    
    String getProperty(String key) {
        if (this == root || properties.containsKey(key)) {
            return properties.getProperty(key);
        } else {
            return parent.getProperty(key);
        }
    }
    
    void setProperty(String key, String value) {
        if (!parent.getProperty(key).equals(value)) {
            properties.setProperty(key, value);
        } else {
            properties.remove(key);
        }
    }

    ConfigProfile deriveUserProfile(String name) {
        ConfigProfile newProfile = new ConfigProfile(Type.USER, name);
        
        ConfigProfile reference;
        if (isFinal()) {
            // set reference for final to the own parent
            reference = parent;
        } else { 
            // otherwise to this 
            reference = this;
        }
        newProfile.setParent(reference);

        // copy properties
        for (String key:properties.stringPropertyNames()){
            if (!key.equals(PARENT_KEY) && !key.equals(FINAL_KEY)) {
                newProfile.setProperty(key, properties.getProperty(key));
            }
        }
        
        return newProfile;
    }
    
    boolean load() {
        // loaded is set independent of success
        loaded = true;
        // ... the same for clearing the current selection
        properties.clear();
        
        // loading
        boolean result;
        if (type == Type.USER) {
            result = loadUser();
        } else {
            result = loadResource();
        }

        // post-load processing
        // set parent according to properties or root
        if (Util.hasValue(properties.getProperty(PARENT_KEY))) {
            setParent(properties.getProperty(PARENT_KEY));
        }
        if (parent == null) {
            setParent(root);
        }
        
        // set save directory to the working directory for predefined values 
        // TODO: This is a hack as workaround to be replaced in the future
        if (type == Type.PREDEFINED && !Util.hasValue(properties.getProperty("save.directory"))) {
            properties.put("save.directory", System.getProperty("user.dir"));
        }
        
        // check if parent has been loaded, otherwise load parent
        if (!parent.isLoaded()) {
            result = result && parent.load();
        }
        
        return result;
    }
    
    private boolean loadUser() {
        File folder = SystemOS.get().getConfigurationFolder(false);
        if (folder == null) {
            return false;
        } else {
            File profile = new File(folder, name + PROFILE_EXTENSION);
            return loadProperties(properties, profile.getAbsolutePath(), false);
        }   
    }
    
    private boolean loadResource(){
        String filePath = null;
        switch(type) {
        case SYSTEM:
            filePath = PREDEFINED_FOLDER + name;
            break;
        case PREDEFINED:
            filePath = PREDEFINED_FOLDER + name + PREDEFINED_EXTENSION ;
            break;
        }
        return loadProperties(properties, filePath, true);
    }
    
    boolean store() {
        if (type != Type.USER) return false;
        loaded = true;
        File folder = SystemOS.get().getConfigurationFolder(true);
        if (folder == null) {
            return false; 
        } else {
            File profile = new File(folder, name + PROFILE_EXTENSION);
            return storeProperties(properties, profile);
        }
    }

    static boolean loadProperties(Properties properties, String filePath, boolean resource) {
        try {
            log.info("Loading properties from file " + filePath);
            InputStream inFile;
            if (resource) {
                inFile = ConfigProfile.class.getClassLoader().getResourceAsStream(filePath);  
            } else {
                inFile = new FileInputStream(filePath);
            }
            properties.load(inFile);
        } catch (Exception e) {
            log.error(e + " whilst loading properties file "
                               + filePath, e);
            return false;
        }
        return true;
    }
    
    static boolean storeProperties(Properties properties, File file) {
        boolean result = true;
        try { 
            properties.store(new FileOutputStream(file), "Automatically generated, do not edit");
            log.info("Storing properties to file " + file.getAbsolutePath());
        } catch (IOException e) {
            log.error(e + " whilst storing properties file "
                    + file.getAbsolutePath());
            result = false;
        }
        return result;
    }
    
    
}


