/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/Attic/GUIBar.java,v 1.1 2009/11/06 20:23:53 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;

/**
 * This class draws a company's token.
 */

public class GUIBar {

    private Color color = Color.BLACK;
    private int x1, x2;
    private int y1, y2;
    private String name;
    private static final int STROKE_WIDTH = 5;

    public static final int DEFAULT_LENGTH = 64;
    public static final int DEFAULT_WIDTH = 12;
    public static final int DEFAULT_X_COORD = 1;
    public static final int DEFAULT_Y_COORD = 1;

    public GUIBar(String name, int x1, int y1,
            int x2, int y2) {
        super();

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.name = name;
    }

    public void drawBar(Graphics2D g2d) {
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();

        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(STROKE_WIDTH));
        g2d.drawLine(x1, y1, x2, y2);

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }

    public Color getColor() {
        return color;
    }

	public String getName() {
        return name;
    }

}
