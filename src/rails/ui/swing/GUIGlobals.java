package rails.ui.swing;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.Config;
import rails.util.Util;

/**
 * Class GUISettings stores some default settings for Graphics:
 * - Scale Information
 * - Antialiasing
 */

public class GUIGlobals {
    private static final Logger log = 
            LoggerFactory.getLogger(GUIGlobals.class);
    
    private static double scale;
    private static double fontsScale;
    private static RenderingHints renderingHints;

    static {
        initMapScale();
        initFontsScale();
        initRenderingHints();
    }

    public static double getMapScale() {
        return scale;
    }

    public static double getFontsScale() {
        return fontsScale;
    }
    
    public static void setRenderingHints(Graphics2D g) {
        g.setRenderingHints(renderingHints);
    }
    
    public static RenderingHints getRenderingHints() {
        return renderingHints;
    }
    
    /**
     * Set the scale so that the MasterBoard fits on the screen. Default scale
     * should be 15 for screen resolutions with height 1000 or more. For less,
     * scale it down linearly.
     * 
     */
    private static void initMapScale() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        scale = 1.0d;
//        if (d.height < 1000) {
//            scale = 1.0d * d.height / 1000;
//        } else {
//            scale = 1.0d;
//        }
        log.debug("GUI-Scale set to " + scale +" due to screensize of " + d);
    }

    public static void initFontsScale() {
        String fontScaleString = Config.getGameSpecific("font.ui.scale");
        if (Util.hasValue(fontScaleString)) {
            try {
                fontsScale = Double.parseDouble(fontScaleString);
            } catch (NumberFormatException e) {
                fontsScale = 1;
            }
        } else {
            fontsScale = 1;
        }
        log.debug("Fonts-Scale set to " + fontsScale);
    }
    
    public static void initRenderingHints() {
        renderingHints = new RenderingHints(null);
        renderingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        renderingHints.put(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        renderingHints.put(RenderingHints.KEY_DITHERING,
                RenderingHints.VALUE_DITHER_DISABLE);
        renderingHints.put(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        renderingHints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    
    
}
