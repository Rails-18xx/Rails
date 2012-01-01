package rails.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;

import javax.swing.Icon;

/** Inner class Tokencon is used to draw a token on the Upgrade chart. */
class Tokencon implements Icon {

    public static final int DEFAULT_DIAMETER = 21;

    private int diameter;
    private Color fgColour;
    private Color bgColour;
    private String text;

    public Tokencon(int diameter, Color fgColour, Color bgColour, String text) {

        this.diameter = diameter;
        this.fgColour = fgColour;
        this.bgColour = bgColour;
        this.text = text;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {

        Ellipse2D.Double circle =
                new Ellipse2D.Double(x, y, diameter, diameter);
        Graphics2D g2d = (Graphics2D) g;
        Color oldColour = g2d.getColor();

        g2d.setColor(bgColour);
        g2d.fill(circle);
        g2d.setColor(Color.BLACK);
        g2d.draw(circle);

        g2d.setColor(oldColour);
        GUIToken.drawTokenText(text, g, fgColour, 
                new Point((int)(circle.x + diameter/2),(int)(circle.y + diameter/2)), 
                diameter);
    }

    public int getIconWidth() {
        return diameter;
    }

    public int getIconHeight() {
        return diameter;
    }
}
