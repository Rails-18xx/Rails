/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/GUIToken.java,v 1.11 2010/01/31 22:22:33 macfreek Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.geom.Ellipse2D;

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

    private static final Font smallTokenFont = new Font("Helvetica", Font.BOLD, 8);
    private static final Font tokenFont = new Font("Helvetica", Font.BOLD, 10);
    private static final Font largeTokenFont = new Font("Helvetica", Font.BOLD, 12);

    @Override
    public void paintComponent(Graphics g) {
        clear(g);
        Graphics2D g2d = (Graphics2D) g;

        drawToken(g2d);

    }

    public void drawToken(Graphics2D g2d) {
        Color oldColor = g2d.getColor();
        Font oldFont = g2d.getFont();
        double tokenScale = diameter / DEFAULT_DIAMETER;

        g2d.setColor(Color.BLACK);
        g2d.draw(circle);
        g2d.setColor(bgColor);
        g2d.fill(circle);

        Font font = getTokenFont(name.length());
        g2d.setFont(new Font("Helvetica", Font.BOLD,
                (int) (font.getSize() * tokenScale)));
        g2d.setColor(fgColor);
        g2d.drawString(name, (int) (circle.x + (12 - 3*name.length()) * tokenScale),
                (int) (circle.y + 14 * tokenScale));

        g2d.setColor(oldColor);
        g2d.setFont(oldFont);
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

    public static Font getTokenFont (int size) {
        if (size <= 2) {
            return largeTokenFont;
        } else if (size <= 4) {
            return tokenFont;
        } else {
            return smallTokenFont;
        }
    }
}
