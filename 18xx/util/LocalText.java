package util;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class LocalText extends ResourceBundle
{

	public static final String version = "1.0.1"; // FIXME: This should be
													// configured via XML.

	protected static String language = "en";
	protected static String country = "";
	protected static String localeCode = language;
	protected static Locale locale;
	protected static ResourceBundle localisedText;

	protected static Logger log = Logger.getLogger(LocalText.class.getPackage().getName());

	public static String getText(String key) {
	    return getText (key, null);
	}
	
	public static String getText (String key, Object parameter) {
	    return getText (key, new Object[] {parameter});
	}
	
	public static String getText (String key, Object[] parameters) 
	{
	    String result = "";

		if (key == null || key.length() == 0)
			return "";

		/* Load the texts */
		if (localisedText == null)
		{
			/* Check what locale has been configured, if any.
			 * If not, we use the default assigned above.
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
				if (localeCode.length()>=2) language = localeCode.substring(0,2);
				if (localeCode.length()>=5) country = localeCode.substring(3,5);
			}
			log.debug ("Language="+language+", country="+country
					+", locale="+localeCode);
			
			/* Create the locale and get the resource bundle. */
			locale = new Locale(language, country);
			localisedText = ResourceBundle.getBundle("LocalisedText", locale);
		}

		/* If the key contains a space, something is wrong, check who did that! */
		if (key.indexOf(" ") > -1)
		{
			try
			{
				throw new Exception("Invalid resource key '" + key + "'");
			}
			catch (Exception e)
			{
				// System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		/* Find the text */
		try
		{
			result = localisedText.getString(key);
		}
		catch (MissingResourceException e)
		{
			System.out.println("Missing text for key " + key + " in locale "
					+ locale.getDisplayName() + " (" + localeCode + ")");
			/* If the text is not found, return the key in brackets */
			return "<" + key + ">";
		}
		
		if (parameters != null) {
		    result = MessageFormat.format (result, parameters);
		}
		
		return result;

	}

	public static void setLocale(String localeCode)
	{

		LocalText.localeCode = localeCode;
		String[] codes = localeCode.split("_");
		if (codes.length > 0)
			language = codes[0];
		if (codes.length > 1)
			country = codes[1];

	}

	public Enumeration getKeys()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Locale getLocale()
	{
		return locale;
	}

	protected Object handleGetObject(String arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
