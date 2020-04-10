/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/LocalText.java,v 1.7 2010/03/23 18:45:16 stefanfrey Exp $*/
package net.sf.rails.common;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocalText extends ResourceBundle {

    private static final String TEST_LOCALE = "te_ST";

    protected static String language = "en";

    protected static String country = "";

    protected static String localeCode = language;

    protected static Locale locale;

    protected static ResourceBundle localisedText;

    private static final Logger log = LoggerFactory.getLogger(LocalText.class);

    public static String getText(String key) {
        return getText(key, (Object[]) null);
    }

    public static String getText(String key, Object parameter) {
        return getText(key,  new Object[] { parameter });
    }

    public static String getText(String key, Object... parameters) {
        /* If the text is not found, return the key in brackets */
        return getTextExecute(key, "[" + key + "]", true, parameters);
    }

    public static String getTextWithDefault(String key, String defaultText) {
        return getTextExecute(key, defaultText, false, (Object[]) null);
    }

    // actual procedure to retrieve the local text
    private static String getTextExecute(String key, String defaultText, boolean errorOnMissing, Object... parameters) {
        String result = "";

        if (key == null || key.length() == 0) return "";

        /* Load the texts */
        if (localisedText == null) {
            /*
             * Check what locale has been configured, if any. If not, we use the
             * default assigned above.
             */
            String item;
            if (Util.hasValue(item = Config.get("language"))) {
                language = item.toLowerCase();
            }
            if (Util.hasValue(item = Config.get("country"))) {
                country = item.toUpperCase();
                localeCode = language + "_" + country;
            }
            if (Util.hasValue(item = Config.get("locale"))) {
                localeCode = item;
                if (localeCode.length() >= 2)
                    language = localeCode.substring(0, 2);
                if (localeCode.length() >= 5)
                    country = localeCode.substring(3, 5);
            }
            log.debug("Language={}, country={}, locale={}", language, country, localeCode);

            /* Create the locale and get the resource bundle. */
            locale = new Locale(language, country);

            try {
                localisedText = ResourceBundle.getBundle("LocalisedText", locale);
            } catch (MissingResourceException e) {
                log.warn("Unable to locate LocalisedText resource: ", e);
            }
        }

        /* If the key contains a space, something is wrong, check who did that! */
        if ( key.contains(" ") ) {
            try {
                throw new Exception("Invalid resource key '" + key + "'");
            } catch (Exception e) {
                log.warn("caught exception", e);
            }
        }

        // special treatment for test locale
        if (localeCode.equals(TEST_LOCALE)) {
            StringBuilder s = new StringBuilder(key);
            if (parameters != null)
                for (Object o:parameters)
                    s.append(",").append(o.toString());
            return s.toString();
        }

        /* Find the text */
        try {
            result = localisedText.getString(key);
        } catch (Exception e) {
            if (errorOnMissing) {
                log.warn("Missing text for key {} in locale {} ({})", key, locale.getDisplayName(), localeCode);
            }
            return defaultText;
        }

        if (parameters != null) {
            result = MessageFormat.format(result, parameters);
        }

        return result;

    }

    public static void setLocale(String localeCode) {

        LocalText.localeCode = localeCode;
        String[] codes = localeCode.split("_");
        if (codes.length > 0) language = codes[0];
        if (codes.length > 1) country = codes[1];

        // reset localised text
        localisedText = null;
    }

    public Enumeration<String> getKeys() {
        // TODO Auto-generated method stub
        return null;
    }

    public Locale getLocale() {
        return locale;
    }

    protected Object handleGetObject(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
