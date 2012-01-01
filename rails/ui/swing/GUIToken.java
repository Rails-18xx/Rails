/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/GUIToken.java,v 1.11 2010/01/31 22:22:33 macfreek Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

/**
 * This class draws a company's token.
 */

public class GUIToken extends JPanel {
    private static final long serialVersionUID = 1L;
    private Color fgColor, bgColor;
    private Ellipse2D.Double circle;
    private String name;
    private double diameter;

    public static final int DEFAULT_DIAMETER = 21;
    public static final int DEFAULT_X_COORD = 1;
    public static final int DEFAULT_Y_COORD = 1;

    private static final Font tokenFontTemplate = new Font("Helvetica", Font.BOLD, 10);
    /**
     * defined by the ratio of margin to diameter 
     * (eg., 0.2 means that 20% of the diameter is used as margin) 
     */
    private static final double tokenTextMargin = 0.15;

    @Override
    public void paintComponent(Graphics g) {
        clear(g);
        Graphics2D g2d = (Graphics2D) g;

        drawToken(g2d);

    }

    public void drawToken(Graphics2D g2d) {

        Color oldColor = g2d.getColor();
        g2d.setColor(Color.BLACK);
        g2d.draw(circle);
        g2d.setColor(bgColor);
        g2d.fill(circle);
        g2d.setColor(oldColor);

        drawTokenText(name, g2d, fgColor, 
                new Point((int)(circle.x + diameter/2),(int)(circle.y + diameter/2)), 
                diameter);
    }

    protected void clear(Graphics g) {
        super.paintComponent(g);
    }

    public GUIToken(String name) {
        this(Color.BLACK, Color.WHITE, name, DEFAULT_X_COORD, DEFAULT_Y_COORD,
                DEFAULT_DIAMETER);
    }

    public GUIToken(Color fc, Color bc, String name) {
        this(fc, bc, name, DEFAULT_X_COORD, DEFAULT_Y_COORD, DEFAULT_DIAMETER);
    }

    public GUIToken(int x, int y, String name) {
        this(Color.BLACK, Color.WHITE, name, x, y, DEFAULT_DIAMETER);
    }

    public GUIToken(Color fc, Color bc, String name, int x, int y) {
        this(fc, bc, name, x, y, DEFAULT_DIAMETER);
    }

    public GUIToken(Color fc, Color bc, String name, int xCenter, int yCenter,
            double diameter) {
        super();

        fgColor = fc;
        bgColor = bc;
        this.diameter = diameter;

        circle = new Ellipse2D.Double(xCenter - 0.5*diameter,
                yCenter-0.5*diameter, diameter, diameter);

        this.setForeground(fgColor);
        this.setOpaque(false);
        this.setVisible(true);
        this.name = name;
    }

    public Color getBgColor() {
        return bgColor;
    }

    public Ellipse2D.Double getCircle() {
        return circle;
    }

    public Color getFgColor() {
        return fgColor;
    }

    @Override
    public String getName() {
        return name;
    }

    public static void drawTokenText (String text, Graphics g, Color c, Point tokenCenter, double tokenDiameter) {

        //first calculate font size
        double allowedTextDiameter = tokenDiameter * (1 - tokenTextMargin);
        Rectangle2D textBoundsInTemplate = g.getFontMetrics(tokenFontTemplate).getStringBounds(text, g);
        double fontScalingX = allowedTextDiameter / textBoundsInTemplate.getWidth();
        double fontScalingY = allowedTextDiameter / textBoundsInTemplate.getHeight();
        double fontScaling = (fontScalingX < fontScalingY) ? fontScalingX : fontScalingY;
        int fontSize = (int) Math.floor(fontScaling * tokenFontTemplate.getSize());

        //draw text
        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        g.setColor(c);
        g.setFont(tokenFontTemplate.deriveFont((float)fontSize)); //float needed to indicate size (not style)
        Rectangle2D textBounds = g.getFontMetrics().getStringBounds(text, g);
        g.drawString(text,
                tokenCenter.x - (int)textBounds.getCenterX(),
                tokenCenter.y - (int)textBounds.getCenterY());
        g.setColor(oldColor);
        g.setFont(oldFont);

    }
}
