package rails.util;

public class Format {

	private static final String DEFAULT_MONEY_FORMAT = "$@";
	private static String moneyFormat = null;
	static {
		String configFormat = Config.get("money_format");
		if (Util.hasValue(configFormat)
				&& configFormat.matches(".*@.*")) {
			moneyFormat = configFormat;
		}
	}
	
	/* This class is never instantiated */
	private Format() {}
	
	public static String money (int amount) {
		if (moneyFormat == null) moneyFormat = DEFAULT_MONEY_FORMAT;
		return moneyFormat.replaceFirst("@", String.valueOf(amount));
	}
	
	public static void setMoneyFormat (String format) {
		moneyFormat = format;
	}

}
