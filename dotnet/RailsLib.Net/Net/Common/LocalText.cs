using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Common
{
    public class ResourceBundle
    {
        public static ResourceBundle GetBundle(string a, LocalText.Locale l) { return new ResourceBundle(); }
        public string GetString(string t) { return t; }
    }

    public class MissingResourceException : Exception
    {

    }

    public class LocalText
    {
        public class Locale
        {
            public Locale(string a, string b) { }
        }

        private const string TEST_LOCALE = "te_ST";

        protected static string language = "en";

        protected static string country = "";

        protected static string localeCode = language;

        protected static Locale locale;

        protected static ResourceBundle localisedText;

        //protected static Logger log =
        //        Logger.getLogger(LocalText.class.getPackage().getName());

        public static string GetText(string key)
        {
            return GetText(key, null);
        }

        public static string GetText(string key, object parameter)
        {
            return GetText(key, new object[] { parameter });
        }

        public static string GetText(string key, params object[] parameters)
        {
            /* If the text is not found, return the key in brackets */
            return GetTextExecute(key, "<" + key + ">", true, parameters);
        }

        public static string GetTextWithDefault(string key, string defaultText)
        {
            return GetTextExecute(key, defaultText, false, null);
        }

        // actual procedure to retrieve the local text
        private static string GetTextExecute(string key, string defaultText, bool errorOnMissing, params object[] parameters)
        {
            string result = "";

            if (string.IsNullOrEmpty(key)) return "";

            /* Load the texts */
            if (localisedText == null)
            {
                /*
                 * Check what locale has been configured, if any. If not, we use the
                 * default assigned above.
                 */
                string item;
                if (!string.IsNullOrEmpty(item = Config.Get("language")))
                {
                    language = item.ToLower();
                }
                if (!string.IsNullOrEmpty(item = Config.Get("country")))
                {
                    country = item.ToUpper();
                    localeCode = language + "_" + country;
                }
                if (!string.IsNullOrEmpty(item = Config.Get("locale")))
                {
                    localeCode = item;
                    if (localeCode.Length >= 2)
                        language = localeCode.Substring(0, 2);
                    if (localeCode.Length >= 5)
                        country = localeCode.Substring(3, 2);
                }
                //log.debug("Language=" + language + ", country=" + country
                //          + ", locale=" + localeCode);

                /* Create the locale and get the resource bundle. */
                locale = new Locale(language, country);

                try
                {
                    localisedText =
                            ResourceBundle.GetBundle("LocalisedText", locale);
                }
                catch (MissingResourceException)
                {
                    //System.err.println("Unable to locate LocalisedText resource: "
                    //                   + e);
                }
            }

            /* If the key contains a space, something is wrong, check who did that! */
            if (key.IndexOf(" ") > -1)
            {
                try
                {
                    throw new Exception("Invalid resource key '" + key + "'");
                }
                catch (Exception)
                {
                    //e.printStackTrace();
                }
            }

            // special treatment for test locale
            if (localeCode == TEST_LOCALE)
            {
                StringBuilder s = new StringBuilder(key);
                if (parameters != null)
                    foreach (object o in parameters)
                        s.Append("," + o.ToString());
                return s.ToString();
            }

            /* Find the text */
            try
            {
                result = localisedText.GetString(key);
            }
            catch (Exception)
            {
                if (errorOnMissing)
                {
                    //System.out.println("Missing text for key " + key + " in locale "
                    //               + locale.getDisplayName() + " (" + localeCode
                    //               + ")");
                }
                return defaultText;
            }

            if (parameters != null)
            {
                result = string.Format(result, parameters);
            }

            return result;

        }

        public static void SetLocale(string localeCode)
        {

            LocalText.localeCode = localeCode;
            string[] codes = localeCode.Split('_');
            if (codes.Length > 0) language = codes[0];
            if (codes.Length > 1) country = codes[1];

            // reset localized text
            localisedText = null;
        }

        public List<string> HetKeys()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Locale HetLocale()
        {
            return locale;
        }

        protected object HandleGetObject(string arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
