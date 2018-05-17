using System;
using System.Collections.Generic;

/**
 * A profile storing configuration settings
 */
namespace GameLib.Net.Common
{
    public sealed class ConfigProfile : IComparable<ConfigProfile>
    {
        // available profile types
        public enum ProfileType { SYSTEM = 0, PREDEFINED = 1, USER = 2 };
        //private Integer sort; Type(int sort) { this.sort = sort; } };

    // Filename extension of profiles
    public const string PROFILE_EXTENSION = ".rails_profile";
    private const string PREDEFINED_EXTENSION = ".predefined";

    // Locations
    // user inside configuration folder
    private const string PROFILE_FOLDER = "profiles/";
    // predefined inside jar
    private const string PREDEFINED_FOLDER = "data/profiles/";
    
    // predefined default profiles
    private const string ROOT_PROFILE = "root";
    private const string TEST_PROFILE = "test";
    
    // the profile selected at the start ...
    private const string STANDARD_PROFILE = "pbem";
    // ... unless a cli option has been set
    private const string CLI_AND_RECENT_OPTION ="profile";
    
    
    // file that stores the list of predefined profiles
    private const string LIST_OF_PROFILES = "LIST_OF_PROFILES";
    
    // property key of predefined profile in user profile
    private const string PARENT_KEY = "profile.parent";
    private const string FINAL_KEY = "profile.final";

    // map of all profiles
    private static Dictionary<string, ConfigProfile> profiles = new Dictionary<string, ConfigProfile>();

    // root profile
    private static ConfigProfile root;

    // profile type
    private ProfileType type;
    
    // profile name
    private string name;

    // profile properties
    private GameProperties properties = new GameProperties();

    // profile loaded
    private bool loaded = false;

    // profile parent
    private ConfigProfile parent = null;


    public static void LoadRoot()
    {
        root = new ConfigProfile(ProfileType.SYSTEM, ROOT_PROFILE);
        root.Load();
    }

    public static ConfigProfile LoadTest()
    {
        ConfigProfile test = new ConfigProfile(ProfileType.SYSTEM, TEST_PROFILE);
        test.Load();
        return test;
    }

    public static void ReadPredefined()
    {
            GameProperties list; // = new GameProperties();
        string filePath = PREDEFINED_FOLDER + LIST_OF_PROFILES;
            list = GameProperties.LoadFromFile(filePath);
        //Util.loadPropertiesFromResource(list, filePath);
        foreach (String name in list.Properties.Keys) //:list.stringPropertyNames())
        {
            new ConfigProfile(ProfileType.PREDEFINED, name);
        }
    }

    public static void ReadUser()
    {
            //File userFolder = SystemOS.get().getConfigurationFolder(PROFILE_FOLDER, false);
            IGameFile userFolder = GameInterface.Instance.GameFileInterface.GetConfigurationFolder(PROFILE_FOLDER, false);
        if (userFolder == null) return;

            throw new NotImplementedException();
        //FilenameFilter filter = new SuffixFileFilter(PROFILE_EXTENSION, IOCase.SYSTEM);
        //for (String fileName:userFolder.list(filter))
        //{
        //    new ConfigProfile(Type.USER, FilenameUtils.getBaseName(fileName));
        //}
    }

    public static ConfigProfile GetStartProfile()
    {
            ConfigProfile profile;
        // first checks cli
        //ConfigProfile profile = GetProfile(System.getProperty(CLI_AND_RECENT_OPTION));
        //if (profile != null)
        //{
        //    return profile;
        //}
        // second check recent
        profile = GetProfile(Config.GetRecent(CLI_AND_RECENT_OPTION));
        if (profile != null)
        {
            return profile;
        }
        // third return standard profile
        profile = GetProfile(STANDARD_PROFILE);
        if (profile != null)
        {
            return profile;
        }
        // last return root
        return root;
    }

    public static ConfigProfile GetProfile(string name)
    {
        if (name == null) return null;
        if (name.Equals(ROOT_PROFILE)) return root;
        return profiles[name];
    }

    public static IEnumerable<ConfigProfile> GetProfiles()
    {
        return profiles.Values;
    }

    private ConfigProfile(ProfileType type, String name)
    {
        this.type = type;
        this.name = name;
        if (type != ProfileType.SYSTEM)
        {
            profiles[name] =  this;
        }
    }

    public ProfileType GetProfileType()
    {
        return type;
    }

    public string Name
    {
            get
            {
                return name;
            }
    }

    bool IsLoaded
    {
            get
            {
                return loaded;
            }
    }

    bool IsFinal
    {
            get
            {
                EnsureLoad();

                if (!string.IsNullOrEmpty(properties.GetProperty(FINAL_KEY)))
                {
                    return bool.Parse(properties.GetProperty(FINAL_KEY)); //Util.parseBoolean(properties.getProperty(FINAL_KEY));
                }
                return false;
            }
    }

    ConfigProfile SetParent(ConfigProfile parent)
    {
        EnsureLoad();
        this.parent = parent;
        properties.SetProperty(PARENT_KEY, parent.Name);
        return this;
    }

    private ConfigProfile SetParent(string name)
    {
        return SetParent(GetProfile(name));
    }

    public ConfigProfile GetParent()
    {
        EnsureLoad();
        return parent;
    }

    public string GetProperty(string key)
    {
        EnsureLoad();
        if (this == parent || properties.Properties.ContainsKey(key))
        {
            return properties.GetProperty(key);
        }
        else
        {
            return parent.GetProperty(key);
        }
    }

    public void SetProperty(string key, string value)
    {
        EnsureLoad();
        if (parent.GetProperty(key) != null && parent.GetProperty(key).Equals(value))
        {
            properties.Properties.Remove(key);
        }
        else
        {
            properties.SetProperty(key, value);
        }
    }


    public void MakeActive()
    {
        EnsureLoad();
        // and store it to recent
        Config.StoreRecent(CLI_AND_RECENT_OPTION, Name);
    }

    public ConfigProfile DeriveUserProfile(string name)
    {
        EnsureLoad();

        ConfigProfile newProfile = new ConfigProfile(ProfileType.USER, name);
        newProfile.loaded = true; // the new profile is assumed to be loaded

        ConfigProfile reference;
        if (IsFinal)
        {
            // set reference for to the own parent
            reference = parent;
        }
        else
        {
            // otherwise to this 
            reference = this;
        }
        newProfile.SetParent(reference);

        // copy properties
        foreach (var key in properties.Properties.Keys)
        {
            //String key = (String)k;
            if (!key.Equals(PARENT_KEY) && !key.Equals(FINAL_KEY))
            {
                newProfile.SetProperty(key, properties.GetProperty(key));
            }
        }

        return newProfile;
    }

    private void EnsureLoad()
    {
        if (loaded == false)
        {
            Load();
        }
    }

    private bool Load()
    {
        // loaded is set independent of success
        loaded = true;
        // ... the same for clearing the current selection
        properties.Properties.Clear();

        // loading
        bool result;
        if (type == ProfileType.USER)
        {
            result = LoadUser();
        }
        else
        {
            result = LoadResource();
        }

        // post-load processing
        // set parent according to properties or root
        if (!string.IsNullOrEmpty(properties.GetProperty(PARENT_KEY)))
        {
            SetParent(properties.GetProperty(PARENT_KEY));
        }

        if (parent == null)
        {
            SetParent(root);
        }

        // set save directory to the working directory for predefined values 
        // TODO: This is a hack as workaround to be replaced in the future
        if (type == ProfileType.PREDEFINED && string.IsNullOrEmpty(properties.GetProperty("save.directory")))
        {
                //properties.SetProperty("save.directory", System.getProperty("user.dir"));
                throw new NotImplementedException();
        }

        // check if parent has been loaded, otherwise load parent
        if (!parent.IsLoaded)
        {
            result = result && parent.Load();
        }

        return result;
    }

    private bool LoadUser()
    {
            //SystemOS.get().getConfigurationFolder(PROFILE_FOLDER, false);
            IGameFile folder = GameInterface.Instance.GameFileInterface.GetConfigurationFolder(PROFILE_FOLDER, false);
        if (folder == null)
        {
            return false;
        }
        else
        {
                //File profile = new File(folder, name + PROFILE_EXTENSION);
                //return Util.loadProperties(properties, profile);
                properties = GameProperties.LoadFromFile(folder.Name + PROFILE_EXTENSION);
                return true;
        }
    }

    private bool LoadResource()
    {
        string filePath = null;
        switch (type)
        {
            case ProfileType.SYSTEM:
                filePath = PREDEFINED_FOLDER + name;
                break;
            case ProfileType.PREDEFINED:
                filePath = PREDEFINED_FOLDER + name + PREDEFINED_EXTENSION;
                break;
        }
            //return Util.loadPropertiesFromResource(properties, filePath);
            properties = GameProperties.LoadFromFile(filePath);
            return true;
    }

    private IGameFile GetFile()
    {
            throw new NotImplementedException();
        //File folder = SystemOS.get().getConfigurationFolder(PROFILE_FOLDER, true);
        //if (folder == null)
        //{
        //    return null;
        //}
        //else
        //{
        //    return new File(folder, name + PROFILE_EXTENSION);
        //}
    }

    /**
     * stores profile
     * @return true if save was successful
     */
    public bool Store()
    {
        if (type != ProfileType.USER) return false;

        IGameFile file = GetFile();
        if (file != null)
        {
                //return Util.storeProperties(properties, file);
                properties.StoreToFile(file);
                return true;
        }
        else
        {
            return false;
        }
    }

    private List<ConfigProfile> GetChildren()
    {
        List<ConfigProfile> children = new List<ConfigProfile>();
        foreach (ConfigProfile profile in profiles.Values)
        {
            if (profile.GetParent() == this)
            {
                children.Add(profile);
            }
        }
        return children;
    }

    /**
     * delete profile (including deleting the saved file and removing from the map of profiles)
     * @return true if deletion was successful
     */
    public bool Delete()
    {
        // cannot delete parents
        if (type != ProfileType.USER) return false;

        // delete profile file
        bool result;
        IGameFile file = GetFile();
        if (file != null)
        {
            if (file.Delete())
            {
                profiles.Remove(this.name);
                result = true;
            }
            else
            {
                result = false;
            }
        }
        else
        {
            result = false;
        }


        if (result)
        {
            // and reassign and save children
            foreach (ConfigProfile child in GetChildren())
            {
                child.SetParent(parent);
                // and transfer (directly stored) properties
                foreach (var key in properties.Properties.Keys)
                {
                    child.SetProperty(key, properties.Properties[key]);
                }
                child.Store();
            }
        }
        return result;

    }

    private int Compare(ConfigProfile a, ConfigProfile b)
    {
        if (a.type != b.type)
        {
            return a.type.CompareTo(b.type);
        }
        else
        {
            return a.Name.CompareTo(b.Name);
        }
    }

    /**
     * Compares first on Type.sort than on name
     */
    public int CompareTo(ConfigProfile other)
    {
        return Compare(this, other);
    }
}
}
