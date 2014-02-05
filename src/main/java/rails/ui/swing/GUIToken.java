package rails.ui.swing;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.ui.swing.hexmap.GUIHex.HexPoint;

/**
 * This class draws a company's token.
 */

public class GUIToken extends JPanel {
    private static final Logger log = 
            LoggerFactory.getLogger(GUIToken.class);
    
    private static final long serialVersionUID = 1L;
    
    private final Color fgColor, bgColor;
    private final Ellipse2D.Double circle;
    private final String name;
    private final double diameter;

//    private static final Font tokenFontTemplate = new Font("DroidSans", Font.BOLD, 10);
    private static final Font tokenFontTemplate = new Font("Helvetica", Font.BOLD, 10);
    /**
     * defined by the ratio of margin to diameter 
     * (eg., 0.2 means that 20% of the diameter is used as margin) 
     */
    private static final double tokenTextMargin = 0.15;

    public GUIToken(Color fc, Color bc, String name, HexPoint center,
            double diameter) {
        this(fc ,bc, name, center.getX(), center.getY(), diameter);
    }

    public GUIToken(Color fc, Color bc, String name, double c_x, double c_y,
            double diameter) {
        super();

        double x = c_x - 0.5 * diameter;
        double y = c_y - 0.5 * diameter;

        fgColor = fc;
        bgColor = bc;
        this.diameter = diameter;

        circle = new Ellipse2D.Double(x, y, diameter, diameter);

        this.setForeground(fgColor);
        this.setOpaque(false);
        this.setVisible(true);
        this.setBounds((int)Math.round(x), (int)Math.round(y), 
                (int)Math.round(diameter), (int)Math.round(diameter));
        this.name = name;
        log.debug("GUIToken with circle " + circle.getBounds2D());
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        drawToken(g2d);
    }

    public void drawToken(Graphics2D g2d) {
        Color oldColor = g2d.getColor();
        g2d.setColor(Color.BLACK);
        g2d.draw(circle);
        g2d.setColor(bgColor);
        g2d.fill(circle);
        g2d.setColor(oldColor);

        HexPoint tokenCenter = new HexPoint(circle.x + diameter/2,circle.y + diameter/2); 
        drawTokenText(name, g2d, fgColor, tokenCenter, diameter); 
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

    public static void drawTokenText (String text, Graphics g, Color c, HexPoint tokenCenter, double tokenDiameter) {

        //recursion if text contains more than 3 characters
        //not perfect (heuristic!) but good enough for this exceptional case
        if (text.length() > 3) {
            double newTokenDiameter = tokenDiameter / 2 / (1 - tokenTextMargin) * 1.2; 
            HexPoint newTokenCenterA = tokenCenter.translate(0, - tokenDiameter / 4 / 1.3);
            HexPoint newTokenCenterB = tokenCenter.translate(0, tokenDiameter / 4 * 1.1);
            drawTokenText( text.substring(0, text.length() / 2), g, c, 
                    newTokenCenterA, newTokenDiameter); 
            drawTokenText( text.substring(text.length() / 2, text.length()), g, c, 
                    newTokenCenterB, newTokenDiameter); 
            return;
        }
        
        //create condensed font if more than 2 chars in a line
        Font fontTemplate;
        if (text.length() > 2) {
            Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
            attributes.put(TextAttribute.WIDTH, TextAttribute.WIDTH_CONDENSED);
            fontTemplate = tokenFontTemplate.deriveFont(attributes);
        } else {
            fontTemplate = tokenFontTemplate;
        }
        
        //first calculate font size
        double allowedTextDiameter = tokenDiameter * (1 - tokenTextMargin);
        Rectangle2D textBoundsInTemplate = g.getFontMetrics(fontTemplate).getStringBounds(text, g);
        double fontScalingX = allowedTextDiameter / textBoundsInTemplate.getWidth();
        double fontScalingY = allowedTextDiameter / textBoundsInTemplate.getHeight();
        double fontScaling = (fontScalingX < fontScalingY) ? fontScalingX : fontScalingY;
        int fontSize = (int) Math.floor(fontScaling * fontTemplate.getSize());

        //draw text
        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        g.setColor(c);
        g.setFont(fontTemplate.deriveFont((float)fontSize)); //float needed to indicate size (not style)
        Rectangle2D textBounds = g.getFontMetrics().getStringBounds(text, g);
        g.drawString(text,
                (int)Math.round(tokenCenter.getX() - textBounds.getCenterX()),
                (int)Math.round(tokenCenter.getY() - textBounds.getCenterY()));
        g.setColor(oldColor);
        g.setFont(oldFont);

    }
}
