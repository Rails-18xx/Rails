package util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocalText extends ResourceBundle
{

	public static final String version = "1.0.1"; // FIXME: This should be
													// configured via XML.

	protected static String language = "en";
	protected static String country = "";
	protected static String localeCode = language;
	protected static Locale locale;
	protected static ResourceBundle localisedText;

	public static String getText(String key)
	{

		if (key == null || key.length() == 0)
			return "";

		/* Load the texts */
		if (localisedText == null)
		{
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
			return (localisedText.getString(key));
		}
		catch (MissingResourceException e)
		{
			System.out.println("Missing text for key " + key + " in locale "
					+ locale.getDisplayName() + " (" + localeCode + ")");
			/* If the text is not found, return the key in brackets */
			return "<" + key + ">";
		}

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
