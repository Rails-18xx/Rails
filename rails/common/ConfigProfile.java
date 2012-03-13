package rails.common;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.log4j.Logger;

import rails.util.SystemOS;
import rails.util.Util;

/**
 * A profile storing configuration settings
 */

public final class ConfigProfile implements Comparable<ConfigProfile> {

    protected static Logger log =
        Logger.getLogger(ConfigProfile.class.getPackage().getName());

    // available profile types
    public enum Type {SYSTEM(0), PREDEFINED(1), USER(2);
    private Integer sort; Type(int sort) {this.sort = sort;}
    };

    // Filename extension of profiles
    public static final String PROFILE_EXTENSION = ".rails_profile";
    private static final String PREDEFINED_EXTENSION = ".predefined";

    // Locations
    // user inside configuration folder
    private static final String PROFILE_FOLDER = "profiles/";
    // predefined inside jar
    private static final String PREDEFINED_FOLDER = "data/profiles/";

    // predefined default profiles
    private static final String ROOT_PROFILE = "root";
    private static final String TEST_PROFILE = "test";

    // the profile selected at the start ...
    private static final String STANDARD_PROFILE = "pbem";
    // ... unless a cli option has been set
    private static final String CLI_AND_RECENT_OPTION ="profile";


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
        Util.loadPropertiesFromResource(list, filePath);
        for (String name:list.stringPropertyNames()) {
            new ConfigProfile(Type.PREDEFINED, name);
        }
    }

    static void readUser() {
        File userFolder = SystemOS.get().getConfigurationFolder(PROFILE_FOLDER, false);
        if (userFolder == null) return;
        FilenameFilter filter = new SuffixFileFilter(PROFILE_EXTENSION, IOCase.SYSTEM);
        for (String fileName:userFolder.list(filter)) {
            new ConfigProfile(Type.USER, FilenameUtils.getBaseName(fileName));
        }
    }

    static ConfigProfile getStartProfile() {
        // first checks cli
        String profile = System.getProperty(CLI_AND_RECENT_OPTION);
        if (Util.hasValue(profile) && profiles.containsKey(profile)) {
            return profiles.get(profile);
        }
        // second check recent
        profile = Config.getRecent(CLI_AND_RECENT_OPTION);
        if (Util.hasValue(profile) && profiles.containsKey(profile)) {
            return profiles.get(profile);
        }
        // third return standard profile
        return profiles.get(STANDARD_PROFILE);
    }

    static ConfigProfile getProfile(String name) {
        return profiles.get(name);
    }

    static Collection<ConfigProfile>  getProfiles() {
        return profiles.values();
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
        if (parent != null) {
            this.parent = parent;
            properties.setProperty(PARENT_KEY, parent.getName());
        }
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


    void makeActive(){
        // check if is already loaded
        if (!isLoaded()) {
            load();
        }
        // and store it to recent
        Config.storeRecent(CLI_AND_RECENT_OPTION, getName());
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
        File folder = SystemOS.get().getConfigurationFolder(PROFILE_FOLDER, false);
        if (folder == null) {
            return false;
        } else {
            File profile = new File(folder, name + PROFILE_EXTENSION);
            return Util.loadProperties(properties, profile);
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
        return Util.loadPropertiesFromResource(properties, filePath);
    }

    boolean store() {
        if (type != Type.USER) return false;
        loaded = true;
        File folder = SystemOS.get().getConfigurationFolder(PROFILE_FOLDER, true);
        if (folder == null) {
            return false;
        } else {
            File profile = new File(folder, name + PROFILE_EXTENSION);
            return Util.storeProperties(properties, profile);
        }
    }

    private int compare(ConfigProfile a, ConfigProfile b) {
        if (a.type.sort != b.type.sort) {
            return a.type.sort.compareTo(b.type.sort);
        } else {
            return a.getName().compareTo(b.getName());
        }
    }

    /**
     * Compares first on Type.sort than on name
     */
    public int compareTo(ConfigProfile other) {
        return compare(this, other);
    }
}


