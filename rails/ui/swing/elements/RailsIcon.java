package rails.ui.swing.elements;

import java.awt.Image;

import javax.swing.ImageIcon;

import rails.common.LocalText;

/**
 * Enumeration that provides a specific ImageIcon
 * Simply use RailsIcon.{IconName}.icon/description 
 * @author freystef
 */

public enum RailsIcon {
    
    // in parentheses the image file
    AUTOPASS ("","Autopass"),
    BID ("","BID"),
    BUY ("","BUY"),
    BUY_PRIVATE ("","BUY_PRIVATE"),
    BUY_TRAIN ("","BUY_TRAIN"),
    DONE ("","Done"),
    END_OF_GAME_CLOSE_ALL_WINDOWS ("","END_OF_GAME_CLOSE_ALL_WINDOWS"),
    INFO ("Inform.gif","Info"),
    LAY_TILE ("","LayTile"),
    OPERATING_COST ("","OCButtonLabel"),
    PASS ("","PASS"),
    PAYOUT ("","PAYOUT"),
    REDO ("","REDO"),
    REPAY_LOANS ("","RepayLoans"),
    REPORT_MOVE_BACKWARD ("","REPORT_MOVE_BACKWARD"),
    REPORT_MOVE_FORWARD ("","REPORT_MOVE_FORWARD"),
    SELECT_NO_BID ("","SelectNoBid"),
    SET_REVENUE ("","SET_REVENUE"),
    SPLIT ("","SPLIT"),
    UNDO ("","UNDO"),
    WITHOLD ("","WITHHOLD"),
    
    //null meaning all public fields are null
    NULL ();
    
    private final static String IMAGE_PATH = "/rails/ui/images/";
    private final static int SMALL_IMAGE_WIDTH = 16;
    private final static int SMALL_IMAGE_HEIGHT = 16;
    private final String configKey;
    /**
     * icon in original resolution
     */
    public final ImageIcon largeIcon;
    /**
     * icon in restricted / small resolution
     */
    public final ImageIcon smallIcon;
    public final String description;
    
    private RailsIcon() {
        configKey = null;
        largeIcon = null;
        smallIcon = null;
        description = null;
    }
    
    private RailsIcon(String fileName,String configKey) {
        this.configKey = configKey;
        this.description = LocalText.getText(configKey);
        largeIcon = createIcon(fileName,description);
        smallIcon = createSmallIcon(largeIcon);
    }
    
    private ImageIcon createIcon(String fileName, String description) {
        //check whether icon is expected to be found
        //don't write error messages if icon not expected to be found
        if (fileName == null || fileName.equals("")) {
            return null;
        }
        
        //get icon
        String path = IMAGE_PATH + fileName;
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    
    private ImageIcon createSmallIcon(ImageIcon originalIcon) {
        ImageIcon smallIcon = null;
        if (originalIcon != null) {
            Image img = originalIcon.getImage();
            if (img != null) {
                smallIcon = new ImageIcon(
                        img.getScaledInstance(
                                SMALL_IMAGE_WIDTH, 
                                SMALL_IMAGE_HEIGHT,
                                Image.SCALE_SMOOTH
                        ),
                        originalIcon.getDescription()
                );
            }
        }
        return smallIcon;
    }
    
    /**
     * @return The Rails icon associated with the key or, if nothing is found, 
     * RailsConfig.NULL
     */
    public static RailsIcon getByConfigKey(String configKey) {
        if (configKey == null) return RailsIcon.NULL;
        
        RailsIcon ri = null;
        for (RailsIcon r : RailsIcon.values()) {
            //ignore case necessary as both Pass and PASS are used by consumers
            if (configKey.equalsIgnoreCase(r.configKey)) ri = r;
        }
        if (ri == null) {
            ri = RailsIcon.NULL;
        }
        return ri;
    }
    
}
