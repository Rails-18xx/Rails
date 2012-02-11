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
    AUCTION_BUY ("auction_hammer_gavel.png","BUY"),
    AUTOPASS ("control_fastforward_blue.png","Autopass"),
    BID ("money_add.png","BID"),
    BUY_PRIVATE ("money_bag.png","BUY_PRIVATE"),
    BUY_TRAIN ("train.png","BUY_TRAIN"),
    DONE ("accept.png","Done"),
    INFO ("information.png","Info"),
    LAY_TILE ("rails32.png","LayTile"),
    PANEL_OR ("participation_rate.png","Dockable.orWindow.orPanel"),
    PANEL_OR_BUTTONS ("button.png","Dockable.orWindow.buttonPanel"),
    PANEL_MAP ("globe_model.png","Dockable.orWindow.mapPanel"),
    PANEL_MESSAGE ("script.png","Dockable.orWindow.messagePanel"),
    PANEL_REMAINING_TILES ("rails32.png","Dockable.orWindow.remainingTilesPanel"),
    PANEL_UPGRADE ("bricks.png","Dockable.orWindow.upgradePanel"),
    PASS ("control_play_blue.png","PASS"),
    PAYOUT ("traffic_lights_green.png","PAYOUT"),
    REDO ("arrow_redo.png","REDO"),
    REPAY_LOANS ("cash_stack.png","RepayLoans"),
    REPORT_MOVE_BACKWARD ("clock_delete.png","REPORT_MOVE_BACKWARD"),
    REPORT_MOVE_FORWARD ("clock_add.png","REPORT_MOVE_FORWARD"),
    SELECT_NO_BID ("hand_property.png","SelectNoBid"),
    SET_REVENUE ("coins_in_hand.png","SET_REVENUE"),
    SPLIT ("traffic_lights_yellow.png","SPLIT"),
    UNDO ("arrow_undo.png","UNDO"),
    WITHHOLD ("traffic_lights_red.png","WITHHOLD"),
    
    //no icons by purpose
    END_OF_GAME_CLOSE_ALL_WINDOWS ("","END_OF_GAME_CLOSE_ALL_WINDOWS"),
    OPERATING_COST ("","OCButtonLabel"),
    
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
