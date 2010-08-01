package rails.ui.swing.elements;

import javax.swing.ImageIcon;

/**
 * Enumeration that provides a specific ImageIcon
 * Simply use RailsIcon.{IconName}.create 
 * @author freystef
 */

public enum RailsIcon {
    
    // in parentheses the image file
    INFO ("Inform.gif");

    private final static String IMAGE_PATH = "/rails/ui/images/"; 
    public final ImageIcon icon;
    
    private RailsIcon(String fileName) {
        String path = IMAGE_PATH + fileName;
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            icon = new ImageIcon(imgURL, "Info");
        } else {
            System.err.println("Couldn't find file: " + path);
            icon = null;
        }
    }
    
}
