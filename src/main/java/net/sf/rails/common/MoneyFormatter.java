/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Format.java,v 1.3 2008/06/04 19:00:39 evos Exp $*/
package net.sf.rails.common;

import net.sf.rails.util.Util;

public class MoneyFormatter {

    private static final String DEFAULT_MONEY_FORMAT = "$@";
    private static String moneyFormat = null;

    static {
        String configFormat = Config.get("money_format");
        if (Util.hasValue(configFormat) && configFormat.matches(".*@.*")) {
            moneyFormat = configFormat;
        }
    }

    /* This class is never instantiated */
    private MoneyFormatter() {}

    public static String money(int amount) {
        if (moneyFormat == null) moneyFormat = DEFAULT_MONEY_FORMAT;
        return moneyFormat.replaceFirst("@", String.valueOf(amount));
    }

    public static void setMoneyFormat(String format) {
        moneyFormat = format;
    }

}
