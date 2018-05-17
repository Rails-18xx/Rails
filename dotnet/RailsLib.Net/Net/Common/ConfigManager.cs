using GameLib.Net.Common.Parser;
using GameLib.Net.Game;
using System;
using System.Collections.Generic;

/**
 * ConfigManager is a utility class that collects all functions
 * used to define and control configuration options
 * 
 * It is a rewrite of the previously used static class Config
 */

namespace GameLib.Net.Common
{
    public class ConfigManager : IConfigurable
    {
        // STATIC CONSTANTS
        private static Logger<ConfigManager> log = new Logger<ConfigManager>();

        //  XML setup
        private const string CONFIG_XML_DIR = "data";
        private const string CONFIG_XML_FILE = "Properties.xml";
        private const string CONFIG_TAG = "Properties";
        private const string SECTION_TAG = "Section";
        private const string ITEM_TAG = "Property";

        // Recent property file
        private const string RECENT_FILE = "rails.recent";

        // singleton configuration for ConfigManager
        private static ConfigManager instance = new ConfigManager();

        // INSTANCE DATA

        // version string and development flag
        private string version = "unknown";
        private bool develop = false;
        private string buildDate = "unknown";

        // configuration items: replace with Multimap in Rails 2.0
        private Dictionary<string, List<ConfigItem>> configSections = new Dictionary<string, List<ConfigItem>>();

        // recent data
        private GameProperties recentData;

        // profile storage
        private ConfigProfile activeProfile;


        public static void InitConfiguration(bool test)
        {
            try
            {
                // Find the config tag inside the the config xml file
                // the last arguments refers to the fact that no GameOptions are required
                string xml = GameInterface.Instance.XmlLoader.LoadXmlFile(CONFIG_XML_FILE, CONFIG_XML_DIR);
                Tag configTag =
                        Tag.FindTopTagInFile(xml, CONFIG_TAG, null);
                log.Debug("Opened config xml, filename = " + CONFIG_XML_FILE);
                instance.ConfigureFromXML(configTag);
            }
            catch (ConfigurationException e)
            {
                log.Error("Configuration error in setup of " + CONFIG_XML_FILE + ", exception = " + e);
            }

            if (test)
            {
                instance.InitTest();
            }
            else
            {
                instance.Init();
            }
        }


        /**
         * @return singleton instance of ConfigManager
         */
        public static ConfigManager Instance
        {
            get
            {
                return instance;
            }
        }

        // private constructor to allow only creation of a singleton
        private ConfigManager()
        {
            recentData = new GameProperties();
        }

        /** 
         * Reads the config.xml file that defines all config items
         */
        public void ConfigureFromXML(Tag tag)
        {

            // find sections
            List<Tag> sectionTags = tag.GetChildren(SECTION_TAG);
            if (sectionTags != null)
            {
                foreach (Tag sectionTag in sectionTags)
                {
                    // find name attribute
                    string sectionName = sectionTag.GetAttributeAsString("name");
                    if (string.IsNullOrEmpty(sectionName)) continue;

                    // find items
                    List<Tag> itemTags = sectionTag.GetChildren(ITEM_TAG);
                    if (itemTags == null || itemTags.Count == 0) continue;
                    List<ConfigItem> sectionItems = new List<ConfigItem>();
                    foreach (Tag itemTag in itemTags)
                    {
                        sectionItems.Add(new ConfigItem(itemTag));
                    }
                    configSections[sectionName] = sectionItems;
                }
            }

        }


        public void FinishConfiguration(RailsRoot parent)
        {
            // do nothing
        }

        private void Init()
        {

            // load recent data
            IGameFile recentFile = GameInterface.Instance.GameFileInterface.GetConfigurationFolder(false); //new File(SystemOS.get().getConfigurationFolder(false), RECENT_FILE);
            recentData = GameProperties.LoadFromFile(recentFile); //Util.loadProperties(recentData, recentFile);

            // define profiles
            ConfigProfile.ReadPredefined();
            ConfigProfile.ReadUser();

            // load root profile
            ConfigProfile.LoadRoot();

            // change to start profile (cli, recent or default)
            ChangeProfile(ConfigProfile.GetStartProfile());

            InitVersion();
        }

        private void InitTest()
        {
            ConfigProfile.LoadRoot();
            activeProfile = ConfigProfile.LoadTest();
            InitVersion();
        }

        private void InitVersion()
        {
            // TODO: Check if this is the right place for this
            /* Load version number and develop flag */
            GameProperties versionNumber; // = new Properties();
            //Util.loadPropertiesFromResource(versionNumber, "version.number");
            versionNumber = GameProperties.LoadFromFile("version.number");

            string sVersion = versionNumber.GetProperty("version");
            if (!string.IsNullOrEmpty(sVersion))
            {
                this.version = sVersion;
            }

            string sDevelop = versionNumber.GetProperty("develop");
            if (!string.IsNullOrEmpty(sDevelop))
            {
                this.develop = sDevelop != "";
            }

            string sBuildDate = versionNumber.GetProperty("buildDate");
            if (!string.IsNullOrEmpty(sBuildDate))
            {
                this.buildDate = sBuildDate;
            }
        }

        /**
        * @return version id (including a "+" attached if development)
*/
        public string Version
        {
            get
            {
                if (develop)
                {
                    return version + "+";
                }
                else
                {
                    return version;
                }
            }
        }

        /**
         * @return true if development version
         */
        public bool Develop
        {
            get
            {
                return develop;
            }
        }

        /**
         * @return the buildDate
         */
        public string BuildDate
        {
            get
            {
                return buildDate;
            }
        }

        public string GetValue(string key, string defaultValue)
        {

            // get value from active profile (this escalates)
            string value = activeProfile.GetProperty(key);
            if (!string.IsNullOrEmpty(value))
            {
                return value.Trim();
            }
            else
            {
                return defaultValue;
            }
        }

        public string ActiveProfile
        {
            get
            {
                return activeProfile.Name;
            }
        }

        public string ActiveParent
        {
            get
            {
                return activeProfile.GetParent().Name;
            }
        }

        public bool IsActiveUserProfile
        {
            get
            {
                return activeProfile.GetProfileType() == ConfigProfile.ProfileType.USER;
            }
        }

        public List<string> GetProfiles()
        {
            // sort and convert to names
            List<ConfigProfile> profiles = new List<ConfigProfile>(ConfigProfile.GetProfiles());
            profiles.Sort();
            List<string> profileNames = new List<string>();
            foreach (ConfigProfile profile in profiles)
            {
                profileNames.Add(profile.Name);
            }
            return profileNames;
        }

        public Dictionary<string, List<ConfigItem>> ConfigSections
        {
            get
            {
                return configSections;
            }
        }

        public int GetMaxElementsInPanels()
        {
            int maxElements = 0;
            foreach (List<ConfigItem> panel in configSections.Values)
            {
                maxElements = Math.Max(maxElements, panel.Count);
            }
            log.Debug("Configuration sections with maximum elements of " + maxElements);
            return maxElements;
        }

        private void ChangeProfile(ConfigProfile profile)
        {
            activeProfile = profile;
            activeProfile.MakeActive();

            // define configItems
            foreach (List<ConfigItem> items in configSections.Values)
            {
                foreach (ConfigItem item in items)
                {
                    item.CurrentValue = GetValue(item.name, null);
                }
            }
        }

        public void ChangeProfile(string profileName)
        {
            ChangeProfile(ConfigProfile.GetProfile(profileName));
        }

        /**
         * updates the user profile according to the changes in configItems
         */
        public bool SaveProfile(bool applyInitMethods)
        {
            foreach (List<ConfigItem> items in configSections.Values)
            {
                foreach (ConfigItem item in items)
                {
                    // if item has changed ==> change profile and call init Method
                    if (item.HasChanged)
                    {
                        activeProfile.SetProperty(item.name, item.NewValue);
                        log.Debug("User properties for = " + item.name + " set to value = " + item.CurrentValue);
                        item.CallInitMethod(applyInitMethods);
                        item.ResetValue();
                    }
                }
            }
            return activeProfile.Store();
        }

        public bool SaveNewProfile(string name, bool applyInitMethods)
        {
            activeProfile = activeProfile.DeriveUserProfile(name);
            return SaveProfile(applyInitMethods);
        }

        public bool DeleteActiveProfile()
        {
            if (activeProfile.Delete())
            {
                activeProfile = activeProfile.GetParent();
                return true;
            }
            else
            {
                return false;
            }
        }

        public string GetRecent(string key)
        {
            // get value from active profile (this escalates)
            string value = recentData.GetProperty(key);
            if (!string.IsNullOrEmpty(value))
            {
                return value.Trim();
            }
            else
            {
                return null;
            }
        }

        public bool StoreRecent(string key, string value)
        {
            // check conditions
            if (key == null) return false;
            if (GetRecent(key) == null || !GetRecent(key).Equals(value))
            {
                if (value == null)
                {
                    recentData.Properties.Remove(key);
                }
                else
                {
                    recentData.SetProperty(key, value);
                }
                //File recentFile = new File(SystemOS.get().getConfigurationFolder(true), RECENT_FILE);
                //return Util.storeProperties(recentData, recentFile);
                IGameFile recentFile = GameInterface.Instance.GameFileInterface.GetConfigurationFolder(true);
                recentData.StoreToFile(recentFile);
                return true;
            }
            // nothing has changed
            return true;
        }

    }
}
