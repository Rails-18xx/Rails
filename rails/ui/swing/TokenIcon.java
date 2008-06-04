package rails.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.Icon;

/** Inner class TokenIcon is used to draw a token on the Upgrade chart. */
class TokenIcon implements Icon {

    public static final int DEFAULT_DIAMETER = 21;

    private int diameter;
    private Color fgColour;
    private Color bgColour;
    private String text;

    public TokenIcon(int diameter, Color fgColour, Color bgColour, String text) {

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
        Font oldFont = g2d.getFont();
        double tokenScale = diameter / DEFAULT_DIAMETER;

        g2d.setColor(bgColour);
        g2d.fill(circle);
        g2d.setColor(Color.BLACK);
        g2d.draw(circle);

        g2d.setFont(new Font("Helvetica", Font.BOLD, (int) (8 * tokenScale)));
        g2d.setColor(fgColour);
        // g2d.drawString(name, 3, 14);
        g2d.drawString(text, (int) (circle.x + 2 * tokenScale),
                (int) (circle.y + 14 * tokenScale));

        g2d.setColor(oldColour);
        g2d.setFont(oldFont);

    }

    public int getIconWidth() {
        return diameter;
    }

    public int getIconHeight() {
        return diameter;
    }
}
