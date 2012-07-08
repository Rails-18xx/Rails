/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/GUIHex.java,v 1.45 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing.hexmap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.algorithms.RevenueBonusTemplate;
import rails.game.Bank;
import rails.game.BaseToken;
import rails.game.BonusToken;
import rails.game.MapHex;
import rails.game.PrivateCompany;
import rails.game.PublicCompany;
import rails.game.Station;
import rails.game.Stop;
import rails.game.Tile;
import rails.game.TileOrientation;
import rails.game.Token;
import rails.game.state.Observable;
import rails.game.state.Observer;
import rails.ui.swing.GUIToken;
import rails.util.Util;

/**
 * Base class that holds common components for GUIHexes of all orientations.
 */

public class GUIHex implements Observer {

    public static final double SQRT3 = Math.sqrt(3.0);
    public static double NORMAL_SCALE = 1.0;
    public static double SELECTABLE_SCALE = 0.9;
    public static double SELECTED_SCALE = 0.8;

    public static int NORMAL_TOKEN_SIZE = 15;
    public static double TILE_GRID_SCALE = 14.0;
    public static double CITY_SIZE = 16.0;

    public static Color BAR_COLOUR = Color.BLUE;
    public static int BAR_WIDTH = 5;

    public static void setScale(double scale) {
        NORMAL_SCALE = scale;
        SELECTABLE_SCALE = 0.9 * scale;
        SELECTED_SCALE = 0.8 * scale;
    }

    protected MapHex model;
    protected GeneralPath innerHexagonSelected;
    protected GeneralPath innerHexagonSelectable;
    protected static final Color highlightColor = Color.red;
    protected static final Color selectableColor = Color.red;
    protected Point center;
    /** x and y coordinates on the map */
    protected int x, y;
    protected double zoomFactor = 1.0;
    protected int tokenDiameter = NORMAL_TOKEN_SIZE;

    protected HexMap hexMap; // Containing this hex
    protected String hexName;
    protected int currentTiled;
    protected int originalTiled;
    protected int currentTileOrientation;
    protected String tileFilename;
    protected Tile currentTile;

    protected GUITile currentGUITile = null;
    protected GUITile provisionalGUITile = null;
    protected boolean upgradeMustConnect;

    protected List<Token> offStationTokens;
    protected List<Integer> barStartPoints;

    protected GUIToken provisionalGUIToken = null;

    protected double tileScale = NORMAL_SCALE;

    protected String toolTip = "";

    // GUI variables
    double[] xVertex = new double[6];
    double[] yVertex = new double[6];
//    double len;
    GeneralPath hexagon;
    Rectangle rectBound;
    int baseRotation = 0;

    /** Globally turns antialiasing on or off for all hexes. */
    static boolean antialias = true;
    /** Globally turns overlay on or off for all hexes */
    static boolean useOverlay = true;
    // Selection is in-between GUI and rails.game state.
    private boolean selected;
    private boolean selectable;
    
    /** Is tile actually painted (if not, there should be an underlying map image) */
    protected boolean tilePainted = true;

    protected static Logger log =
            LoggerFactory.getLogger(GUIHex.class.getPackage().getName());

    public GUIHex(HexMap hexMap, double cx, double cy, int scale,
            int xCoord, int yCoord) {
        this.hexMap = hexMap;
        this.x = xCoord;
        this.y = yCoord;

        scaleHex (cx, cy, scale, 1.0);

    }

    public void scaleHex (double cx, double cy, int scale, double zoomFactor) {

        this.zoomFactor = zoomFactor;
        tokenDiameter = (int)Math.round(NORMAL_TOKEN_SIZE * zoomFactor);

        if (hexMap.getMapManager().getTileOrientation() == TileOrientation.EW) {
            /* The numbering is unusual:
             *         0
             *        / \
             *       5   1
             *       |   |
             *       4   2
             *        \ /
             *         3
             */
            xVertex[0] = cx + SQRT3 * scale;
            yVertex[0] = cy + scale;
            xVertex[1] = cx + 2 * SQRT3 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 2 * SQRT3 * scale;
            yVertex[2] = cy - 2 * scale;
            xVertex[3] = cx + SQRT3 * scale;
            yVertex[3] = cy - 3 * scale;
            xVertex[4] = cx;
            yVertex[4] = cy - 2 * scale;
            xVertex[5] = cx;
            yVertex[5] = cy;

            baseRotation = 30; // degrees
        } else {
            /* The numbering is unusual:
             *      4--3
             *     /    \
             *    5      2
             *     \    /
             *      0--1
             */
            xVertex[0] = cx;
            yVertex[0] = cy;
            xVertex[1] = cx + 2 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 3 * scale;
            yVertex[2] = cy + SQRT3 * scale;
            xVertex[3] = cx + 2 * scale;
            yVertex[3] = cy + 2 * SQRT3 * scale;
            xVertex[4] = cx;
            yVertex[4] = cy + 2 * SQRT3 * scale;
            xVertex[5] = cx - 1 * scale;
            yVertex[5] = cy + SQRT3 * scale;

            baseRotation = 0;
        }

        hexagon = makePolygon(6, xVertex, yVertex, true);
        rectBound = hexagon.getBounds();

        center =
                new Point((int) ((xVertex[2] + xVertex[5]) / 2),
                        (int) ((yVertex[0] + yVertex[3]) / 2));
        Point2D.Double center2D =
                new Point2D.Double((xVertex[2] + xVertex[5]) / 2.0,
                        (yVertex[0] + yVertex[3]) / 2.0);

        innerHexagonSelected = defineInnerHexagon(0.8, center2D);
        innerHexagonSelectable = defineInnerHexagon(0.9, center2D);
    }

    private GeneralPath defineInnerHexagon(double innerScale, Point2D.Double center2D) {

        AffineTransform at =
                AffineTransform.getScaleInstance(innerScale, innerScale);
        GeneralPath innerHexagon = (GeneralPath) hexagon.createTransformedShape(at);

        // Translate innerHexagon to make it concentric.
        Rectangle2D innerBounds = innerHexagon.getBounds2D();
        Point2D.Double innerCenter =
                new Point2D.Double(innerBounds.getX() + innerBounds.getWidth()
                                   / 2.0, innerBounds.getY()
                                          + innerBounds.getHeight() / 2.0);
        at =
                AffineTransform.getTranslateInstance(center2D.getX()
                                                     - innerCenter.getX(),
                        center2D.getY() - innerCenter.getY());
        innerHexagon.transform(at);

        return innerHexagon;
    }

    /**
     * returns point that corresponds to the definition as networkvertex
     */
    public Point getHexPoint(int side){
        if (side >= 0) { // side
            return new Point((int) xVertex[side],(int) yVertex[side]);
        } else { // station
            return getTokenCenter(0, 0, 0, -side);
        }
    }

    public MapHex getHexModel() {
        return this.model;
    }

    public HexMap getHexMap() {
        return hexMap;
    }

    public Point2D getCityPoint2D(Stop city){
        Point tokenPoint = getTokenCenter(0, 1, 0, city.getNumber() - 1);
        return new Point2D.Double(tokenPoint.getX(), tokenPoint.getY());
    }

    public Point2D getSidePoint2D(int side){
        return new Point2D.Double((xVertex[side] + xVertex[(side+1)%6])/2,
                    (yVertex[side] + yVertex[(side+1)%6])/2);
    }

    public Point2D getCenterPoint2D() {
        return center;
    }

    public void setHexModel(MapHex model) {
        this.model = model;
        currentTile = model.getCurrentTile();
        hexName = model.getId();
        currentTiled = model.getCurrentTile().getNb();
        currentTileOrientation = model.getCurrentTileRotation();
        currentGUITile = new GUITile(currentTiled, this);
        currentGUITile.setRotation(currentTileOrientation);
        toolTip = null;

        model.addObserver(this);
    }

    public void addBar (int orientation) {
        // NOTE: orientation here is its normal value in Rails + 3 (mod 6).
        orientation %= 6;
        if (barStartPoints == null) barStartPoints = new ArrayList<Integer>(2);
        if (hexMap.getMapManager().getTileOrientation() == TileOrientation.EW) {
            barStartPoints.add((5-orientation)%6);
        } else {
            barStartPoints.add((3+orientation)%6);
        }
    }

    public Rectangle getBounds() {
        return rectBound;
    }

    public void setBounds(Rectangle rectBound) {
        this.rectBound = rectBound;
    }

    public boolean contains(Point2D.Double point) {
        return (hexagon.contains(point));
    }

    public boolean contains(Point point) {
        return (hexagon.contains(point));
    }

    public boolean intersects(Rectangle2D r) {
        return (hexagon.intersects(r));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            currentGUITile.setScale(SELECTED_SCALE);
        } else if (!isSelectable()) {
            currentGUITile.setScale(NORMAL_SCALE);
            provisionalGUITile = null;
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        if (selectable) {
            currentGUITile.setScale(SELECTABLE_SCALE);
        } else {
            currentGUITile.setScale(NORMAL_SCALE);
            provisionalGUITile = null;
        }
    }

    public boolean isSelectable() {
        return selectable;
    }

    static boolean getAntialias() {
        return antialias;
    }

    static void setAntialias(boolean enabled) {
        antialias = enabled;
    }

    static boolean getOverlay() {
        return useOverlay;
    }

    public static void setOverlay(boolean enabled) {
        useOverlay = enabled;
    }

    /**
     * Return a GeneralPath polygon, with the passed number of sides, and the
     * passed x and y coordinates. Close the polygon if the argument closed is
     * true.
     */
    static GeneralPath makePolygon(int sides, double[] x, double[] y,
            boolean closed) {
        GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, sides);
        polygon.moveTo((float) x[0], (float) y[0]);
        for (int i = 1; i < sides; i++) {
            polygon.lineTo((float) x[i], (float) y[i]);
        }
        if (closed) {
            polygon.closePath();
        }

        return polygon;
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        tilePainted = provisionalGUITile != null && hexMap.isTilePainted(provisionalGUITile.getTiled()) 
                || currentGUITile != null && hexMap.isTilePainted(currentGUITile.getTiled());
        
        if (getAntialias()) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        Color terrainColor = Color.WHITE;
        if (isSelected()) {
            if (!hexMap.hasMapImage()) {
                g2.setColor(highlightColor);
                g2.fill(hexagon);
            
                g2.setColor(terrainColor);
                g2.fill(innerHexagonSelected);

                g2.setColor(Color.black);
                g2.draw(innerHexagonSelected);
            } else {
                g2.setColor(highlightColor);                
                g2.draw(hexagon);            

                Stroke oldStroke = g2.getStroke();                
                g2.setStroke(new BasicStroke(4));
                                              
                g2.draw(innerHexagonSelected);            
                
                g2.setStroke(oldStroke);                
                g2.setColor(Color.black);
            }
        } else if (isSelectable()) {
            if (!hexMap.hasMapImage()) {
                g2.setColor(selectableColor);
                g2.fill(hexagon);

                g2.setColor(terrainColor);
                g2.fill(innerHexagonSelectable);

                g2.setColor(Color.black);
                g2.draw(innerHexagonSelectable);
            } else {
                g2.setColor(selectableColor);
                                              
                g2.draw(hexagon);            
                
                g2.setColor(Color.black);
            }
        }

        if (tilePainted) paintOverlay(g2);
        paintStationTokens(g2);
        paintOffStationTokens(g2);

        if (!tilePainted) return;
        
        FontMetrics fontMetrics = g2.getFontMetrics();
        if (getHexModel().getTileCost() > 0 ) {
            g2.drawString(
                    Bank.format(getHexModel().getTileCost()),
                    rectBound.x
                            + (rectBound.width - fontMetrics.stringWidth(Integer.toString(getHexModel().getTileCost())))
                            * 3 / 5,
                    rectBound.y
                            + ((fontMetrics.getHeight() + rectBound.height) * 9 / 15));
        }

        Map<PublicCompany, Stop> homes = getHexModel().getHomes();

        if (homes  != null) {
            Stop homeCity;
            Point p;
            for (PublicCompany company : homes.keySet()) {
                if (company.isClosed()) continue;

                // Only draw the company name if there isn't yet a token of that company
                if (model.hasTokenOfCompany(company)) continue;
                homeCity = homes.get(company);
                if (homeCity == null) { // not yet decided where the token will be
                    // find a free slot
                    List<Stop> stops = getHexModel().getStops();
                    for (Stop stop:stops) {
                        if (stop.hasTokenSlotsLeft()) {
                            homeCity = stop;
                            break;
                        }
                    }
                }
                // check the number of tokens laid there already
                p = getTokenCenter (1, homeCity.getTokens().size(), getHexModel().getStops().size(),
                        homeCity.getNumber()-1);
                drawHome (g2, company, p);
            }
        }

        if (getHexModel().isBlockedForTileLays()) {
            List<PrivateCompany> privates =
                    //GameManager.getInstance().getCompanyManager().getAllPrivateCompanies();
            		hexMap.getOrUIManager().getGameUIManager().getGameManager()
            			.getCompanyManager().getAllPrivateCompanies();
            for (PrivateCompany p : privates) {
                List<MapHex> blocked = p.getBlockedHexes();
                if (blocked != null) {
                    for (MapHex hex : blocked) {
                        if (getHexModel().equals(hex)) {
                        	String text = "(" + p.getId() + ")";
                            g2.drawString(
                                  text,
                                  rectBound.x
                                  + (rectBound.width - fontMetrics.stringWidth(text))
                                  * 1 / 2,
                                  rectBound.y
                                  + ((fontMetrics.getHeight() + rectBound.height) * 5 / 15));
                        }
                    }
                }
            }
        }

        if (model.isReservedForCompany()
        		&& currentTiled == model.getPreprintedTiled() ) {
        	String text = "[" + model.getReservedForCompany() + "]";
            g2.drawString(
                  text,
                  rectBound.x
                  + (rectBound.width - fontMetrics.stringWidth(text))
                  * 1 / 2,
                  rectBound.y
                  + ((fontMetrics.getHeight() + rectBound.height) * 5 / 25));
        }

    }

    private void paintOverlay(Graphics2D g2) {
        if (provisionalGUITile != null) {
            if (hexMap.isTilePainted(provisionalGUITile.getTiled())) {
                provisionalGUITile.paintTile(g2, center.x, center.y);
            }
        } else {
            if (hexMap.isTilePainted(currentGUITile.getTiled())) {
                currentGUITile.paintTile(g2, center.x, center.y);
            }
        }
    }

    public void paintBars(Graphics g) {
        if (barStartPoints == null) return;
        Graphics2D g2 = (Graphics2D) g;
        for (int startPoint : barStartPoints) {
            drawBar(g2,
                    (int)Math.round(xVertex[startPoint]),
                    (int)Math.round(yVertex[startPoint]),
                    (int)Math.round(xVertex[(startPoint+1)%6]),
                    (int)Math.round(yVertex[(startPoint+1)%6]));
        }
    }

    protected void drawBar(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();

        g2d.setColor(BAR_COLOUR);
        g2d.setStroke(new BasicStroke(BAR_WIDTH));
        g2d.drawLine(x1, y1, x2, y2);

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }

    private void paintStationTokens(Graphics2D g2) {

        if (getHexModel().getStops().size() > 1) {
            paintSplitStations(g2);
            return;
        }

        int numTokens = getHexModel().getTokens(1).size();
        List<Token> tokens = getHexModel().getTokens(1);

        for (int i = 0; i < tokens.size(); i++) {
            PublicCompany co = ((BaseToken) tokens.get(i)).getParent();
            Point center = getTokenCenter(numTokens, i, 1, 0);
            drawBaseToken(g2, co, center, tokenDiameter);
        }
    }

    private void paintSplitStations(Graphics2D g2) {
        int numStations = getHexModel().getStops().size();
        int numTokens;
        List<Token> tokens;
        Point origin;
        PublicCompany co;

        for (int i = 0; i < numStations; i++) {
            tokens = getHexModel().getTokens(i + 1);
            numTokens = tokens.size();

            for (int j = 0; j < tokens.size(); j++) {
                origin = getTokenCenter(numTokens, j, numStations, i);
                co = ((BaseToken) tokens.get(j)).getParent();
                drawBaseToken(g2, co, origin, tokenDiameter);
            }
        }
    }

    private static int[] offStationTokenX = new int[] { -11, 0 };
    private static int[] offStationTokenY = new int[] { -19, 0 };

    private void paintOffStationTokens(Graphics2D g2) {
        List<Token> tokens = getHexModel().getTokens().items();
        if (tokens == null) return;

        int i = 0;
        for (Token token : tokens) {

            Point origin =
                    new Point(center.x + offStationTokenX[i],
                            center.y + offStationTokenY[i]);
            if (token instanceof BaseToken) {

                PublicCompany co = ((BaseToken) token).getParent();
                drawBaseToken(g2, co, origin, tokenDiameter);

            } else if (token instanceof BonusToken) {

                drawBonusToken(g2, (BonusToken) token, origin);
            }
            if (++i > 1) break;
        }
    }

    private void drawBaseToken(Graphics2D g2, PublicCompany co, Point center, int diameter) {

        GUIToken token =
                new GUIToken(co.getFgColour(), co.getBgColour(), co.getId(),
                        center.x, center.y, diameter);
        token.setBounds(center.x-(int)(0.5*diameter), center.y-(int)(0.5*diameter),
                diameter, diameter);

        token.drawToken(g2);
    }

    private void drawHome (Graphics2D g2, PublicCompany co, Point origin) {

        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();

        double scale = 0.5 * zoomFactor;
        String name = co.getId();

        Font font = GUIToken.getTokenFont(name.length());
        g2.setFont(new Font("Helvetica", Font.BOLD,
                (int) (font.getSize() * zoomFactor * 0.75)));
        g2.setColor(Color.BLACK);
        g2.drawString(name, (int) (origin.x + (-4 - 3*name.length()) * scale),
                (int) (origin.y + 4 * scale));

        g2.setColor(oldColor);
        g2.setFont(oldFont);
    }

    private void drawBonusToken(Graphics2D g2, BonusToken bt, Point origin) {
        Dimension size = new Dimension(40, 40);

        GUIToken token =
                new GUIToken(Color.BLACK, Color.WHITE, "+" + bt.getValue(),
                        origin.x, origin.y, 15);
        token.setBounds(origin.x, origin.y, size.width, size.height);

        token.drawToken(g2);
    }

    public void rotateTile() {
        if (provisionalGUITile != null) {
            provisionalGUITile.rotate(1, currentGUITile, upgradeMustConnect);
        }
    }

    public void forcedRotateTile() {
        provisionalGUITile.setRotation(provisionalGUITile.getRotation() + 1);
    }

    private Point getTokenCenter(int numTokens, int currentToken,
            int numStations, int stationNumber) {
        Point p = new Point(center.x, center.y);

        int cityNumber = stationNumber + 1;
        Station station = model.getStop(cityNumber).getRelatedStation();

        // Find the correct position on the tile
        double x = 0;
        double y = 0;
        double xx, yy;
        int positionCode = station.getPosition();
        if (positionCode != 0) {
            y = TILE_GRID_SCALE * zoomFactor;
            double r = Math.toRadians(30 * (positionCode / 50));
            xx = x * Math.cos(r) + y * Math.sin(r);
            yy = y * Math.cos(r) - x * Math.sin(r);
            x = xx;
            y = yy;
        }

        // Correct for the number of base slots and the token number
        switch (station.getBaseSlots()) {
        case 2:
            x += (-0.5 + currentToken) * CITY_SIZE * zoomFactor;
            break;
        case 3:
            if (currentToken < 2) {
                x += (-0.5 + currentToken) * CITY_SIZE * zoomFactor;
                y += -3 + 0.25 * SQRT3 * CITY_SIZE * zoomFactor;
            } else {
                y -= 3 + 0.5 * CITY_SIZE * zoomFactor;
            }
            break;
        case 4:
            x += (-0.5 + currentToken % 2) * CITY_SIZE * zoomFactor;
            y += (0.5 - currentToken / 2) * CITY_SIZE * zoomFactor;
        }

        // Correct for the tile base and actual rotations
        int rotation = model.getCurrentTileRotation();
        double r = Math.toRadians(baseRotation + 60 * rotation);
        xx = x * Math.cos(r) + y * Math.sin(r);
        yy = y * Math.cos(r) - x * Math.sin(r);
        x = xx;
        y = yy;

        p.x += x;
        p.y -= y;

        // log.debug("New origin for hex "+getName()+" tile
        // #"+model.getCurrentTile().getId()
        // + " city "+cityNumber+" pos="+positionCode+" token "+currentToken+":
        // x="+x+" y="+y);

        return p;
    }

    // Added by Erik Vos
    /**
     * @return Returns the name.
     */
    public String getName() {
        return hexName;
    }

    /**
     * @return Returns the currentTile.
     */
    public Tile getCurrentTile() {
        return currentTile;
    }

    /**
     * @param currentTileOrientation The currentTileOrientation to set.
     */
    public void setTileOrientation(int tileOrientation) {
        this.currentTileOrientation = tileOrientation;
    }

    public String getToolTip() {
        if (toolTip != null)
            return toolTip;
        else
            return getDefaultToolTip();
    }


    private String bonusToolTipText(List<RevenueBonusTemplate> bonuses) {
        StringBuffer tt = new StringBuffer();
        if (bonuses != null) {
            Set<String> bonusNames = new HashSet<String>();
            for (RevenueBonusTemplate bonus:bonuses) {
                if (bonus.getName() == null) {
                    tt.append("<br>Bonus:");
                    tt.append(bonus.getToolTip());
                } else if (!bonusNames.contains(bonus.getName())) {
                    tt.append("<br>Bonus:" + bonus.getName());
                    bonusNames.add(bonus.getName());
                }
            }
        }
        return tt.toString();
    }

    private String getDefaultToolTip() {
        StringBuffer tt = new StringBuffer("<html>");
        tt.append("<b>Hex</b>: ").append(hexName);
        String name = model.getCityName();
        if (Util.hasValue(name)) {
            tt.append(" (").append(name).append(")");
        }
        // For debugging: display x,y-coordinates
        //tt.append("<small> x=" + x + " y="+y+"</small>");
        
        tt.append("<br><b>Tile</b>: ").append(currentTile.getNb());
        
        // For debugging: display rotation
        //tt.append("<small> rot=" + currentTileOrientation + "</small>");

        if (model.hasValuesPerPhase()) {
            tt.append("<br>Value ");
            tt.append(model.getCurrentValueForPhase(hexMap.getPhase())).append(" [");
            int[] values = model.getValuesPerPhase();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) tt.append(",");
                tt.append(values[i]);
            }
            tt.append("]");
        } else if (currentTile.hasStations()) {
            Station st;
            int stopNumber;
            for (Stop stop : model.getStops()) {
                stopNumber = stop.getNumber();
                st = stop.getRelatedStation();
                tt.append("<br>  ").append(st.getType()).append(" ").append(stopNumber)
                    .append(" (").append(model.getConnectionString(stopNumber))
                    .append("): value ");
                tt.append(st.getValue());
                if (st.getBaseSlots() > 0) {
                    tt.append(", ").append(st.getBaseSlots()).append(" slots");
                    List<Token> tokens = model.getTokens(stopNumber);
                    if (tokens.size() > 0) {
                        tt.append(" (");
                        int oldsize = tt.length();
                        for (Token token : tokens) {
                            if (tt.length() > oldsize) tt.append(",");
                            tt.append(token.getId());
                        }
                        tt.append(")");
                    }
                }
                // TEMPORARY
                tt.append(" <small>pos=" + st.getPosition() + "</small>");
            }
            tt.append(bonusToolTipText(currentTile.getRevenueBonuses()));
        }

        // revenueBonuses
        tt.append(bonusToolTipText(model.getRevenueBonuses()));

        String upgrades = currentTile.getUpgradesString(model);
        if (upgrades.equals("")) {
            tt.append("<br>No upgrades");
        } else {
            tt.append("<br><b>Upgrades</b>: ").append(upgrades);
            if (model.getTileCost() > 0)
                tt.append("<br>Upgrade cost: "
                          + Bank.format(model.getTileCost()));
        }

        if (getHexModel().getDestinations() != null) {
            tt.append("<br><b>Destination</b>:");
            for (PublicCompany dest : getHexModel().getDestinations()) {
                tt.append(" ");
                tt.append(dest.getId());
            }
        }


        tt.append("</html>");
        return tt.toString();
    }

    public boolean dropTile(int tileId, boolean upgradeMustConnect) {
        this.upgradeMustConnect = upgradeMustConnect;

        provisionalGUITile = new GUITile(tileId, this);
        /* Check if we can find a valid orientation of this tile */
        if (provisionalGUITile.rotate(0, currentGUITile, upgradeMustConnect)) {
            /* If so, accept it */
            provisionalGUITile.setScale(SELECTED_SCALE);
            toolTip = "Click to rotate";
            return true;
        } else {
            /* If not, refuse it */
            provisionalGUITile = null;
            return false;
        }

    }

    /** forces the tile to drop */
    public void forcedDropTile(int tileId, int orientation) {
        provisionalGUITile = new GUITile(tileId, this);
        provisionalGUITile.setRotation(orientation);
        provisionalGUITile.setScale(SELECTED_SCALE);
        toolTip = "Click to rotate";
    }

    public void removeTile() {
        provisionalGUITile = null;
        setSelected(false);
        toolTip = null;
    }

    public boolean canFixTile() {
        return provisionalGUITile != null;
    }

    public Tile getProvisionalTile() {
        return provisionalGUITile.getTile();
    }

    public int getProvisionalTileRotation() {
        return provisionalGUITile.getRotation();
    }

    public void fixTile() {

        setSelected(false);
        toolTip = null;
    }

    public void removeToken() {
        provisionalGUIToken = null;
        setSelected(false);
        toolTip = null;
    }

    public void fixToken() {
        setSelected(false);
        toolTip = null;
    }

    // FIXME: Why is this called getModel instead of getMapHex?
    public MapHex getModel() {
        return model;
    }

    // Used by Undo/Redo
    public void update(String notification) {
            // The below code so far only deals with tile lay undo/redo.
            // Tokens still to do
            String[] elements = notification.split("/");
            currentTiled = Integer.parseInt(elements[0]);
            currentTileOrientation = Integer.parseInt(elements[1]);
            currentGUITile = new GUITile(currentTiled, this);
            currentGUITile.setRotation(currentTileOrientation);
            currentTile = currentGUITile.getTile();

            hexMap.repaint(getBounds());

            provisionalGUITile = null;

            //log.debug("GUIHex " + model.getName() + " updated: new tile "
            //          + currentTiled + "/" + currentTileOrientation);

            //if (GameUIManager.instance != null && GameUIManager.instance.orWindow != null) {
            //	GameUIManager.instance.orWindow.updateStatus();
            //}
    }
    
    public String toString () {
        return getName() + " (" + currentTile.getId() + ")";
    }

    // FIMXE: Add the code here
    public void update(Observable observable, String id) {
        // TODO Auto-generated method stub
        
    }

}
