using GameLib.Net.Game;

/**
 * Proxy class to the ConfigManager
 */

namespace GameLib.Net.Common
{
    public class Config
    {
        /**
* @return version id (including a "+" attached if development)
*/
        public static string Version
        {
            get
            {
                return ConfigManager.Instance.Version;
            }
        }

        /**
         * @return true if development version
         */
        public static bool Develop
        {
            get
            {
                return ConfigManager.Instance.Develop;
            }
        }

        public static string BuildDate
        {
            get
            {
                return ConfigManager.Instance.BuildDate;
            }
        }

        /**
         * Configuration option (default value is empty string)
         */
        public static string Get(string key)
        {
            return ConfigManager.Instance.GetValue(key, "");
        }

        /**
         * Configuration option with default value
         */
        public static string Get(string key, string defaultValue)
        {
            return ConfigManager.Instance.GetValue(key, defaultValue);
        }

        /**
         * Configuration option: First tries to return {key}.{appendix}, if undefined returns {key}
         */
        public static string GetSpecific(string key, string appendix)
        {
            string value = Get(key + "." + appendix);
            if (!string.IsNullOrEmpty(value))
            {
                return value;
            }
            else
            {
                return Get(key);
            }
        }

        /**
         * Configuration option: First tries to return {key}.{gameName}, if undefined returns {key} 
         */
        public static string GetGameSpecific(string key)
        {
            return GetSpecific(key, RailsRoot.Instance.GameName);
        }

        public static string GetRecent(string key)
        {
            return ConfigManager.Instance.GetRecent(key);
        }

        public static bool StoreRecent(string key, string value)
        {
            return ConfigManager.Instance.StoreRecent(key, value);
        }

    }
}
