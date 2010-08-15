/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/Scale.java,v 1.3 2008/06/04 19:00:32 evos Exp $*/
package rails.ui.swing;

import java.awt.Dimension;
import java.awt.Toolkit;

import rails.util.Config;
import rails.util.Util;

/**
 * Class Scale holds static information used to scale all GUI elements.
 */

public final class Scale {
    public static int scale = 15;
    
    public static double fontScale = 0;

    static {
        fitScreenRes();
    }

    public static int get() {
        return scale;
    }

    public static void set(int scale) {
        Scale.scale = scale;
    }

    public static double getFontScale() {
        if (fontScale == 0) {
            initFromConfiguration();
        }
        return fontScale;
    }
    
    public static void initFromConfiguration() {
        String fontScaleString = Config.getGameSpecific("font.ui.scale");
        if (Util.hasValue(fontScaleString)) {
            try {
                fontScale = Double.parseDouble(fontScaleString);
            } catch (NumberFormatException e) {
                fontScale = 1;
            }
        } else {
            fontScale = 1;
        }
    }
    
    
    /**
     * Set the scale so that the MasterBoard fits on the screen. Default scale
     * should be 15 for screen resolutions with height 1000 or more. For less,
     * scale it down linearly.
     * 
     */
    public static void fitScreenRes() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000) {
            scale = scale * d.height / 1000;
        }
    }
}
