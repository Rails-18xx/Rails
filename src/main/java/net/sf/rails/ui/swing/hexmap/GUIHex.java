package net.sf.rails.ui.swing.hexmap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rails.game.action.LayBaseToken;
import rails.game.action.LayBonusToken;
import net.sf.rails.algorithms.RevenueBonusTemplate;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.HexSide;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapOrientation;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Station;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Tile;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;
import net.sf.rails.ui.swing.GUIGlobals;
import net.sf.rails.ui.swing.GUIToken;

import com.google.common.collect.Lists;


/**
 * Base class that holds common components for GUIHexes of all orientations.
 */

public class GUIHex implements Observer {
    
    /**
     * Static class that describes x-y coordinates for GUIHexes
     */
    public static class HexPoint {
        private final Point2D point;
        
        public HexPoint(double x, double y) {
            this.point = new Point2D.Double(x, y);
        }
        
        public HexPoint(Point2D point) {
           this.point = point;
        }
        
        public Point2D get2D() {
            return point;
        }
        
        public double getX() {
            return point.getX();
        }
        
        public double getY() {
            return point.getY();
        }
        
        @Override
        public String toString() {
            return point.toString();
        }
        
        public HexPoint rotate(double radians) {
            if (radians == 0) return this;
            double x = getX() * Math.cos(radians) + getY() * Math.sin(radians);
            double y = getY() * Math.cos(radians) - getX() * Math.sin(radians);
            return new HexPoint(x, y);
        }
        
        public HexPoint translate(double x, double y) {
            if (x == 0 && y == 0) return this;
            return new HexPoint(this.getX() + x, this.getY() + y);
        }
        
        public static HexPoint middle(HexPoint a, HexPoint b) {
            return new HexPoint((a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0);
        }
        
        public static HexPoint add(HexPoint a, HexPoint b) {
            return new HexPoint(a.getX() + b.getX(), a.getY() + b.getY());
        }
        
        public static HexPoint difference(HexPoint a, HexPoint b) {
            return new HexPoint(a.getX() - b.getX(), a.getY()- b.getY());
        }
        
    }
    
    public static enum State { 
        
        NORMAL(1.0, Color.black), SELECTABLE(0.9, Color.red), SELECTED(0.8, Color.red), INVALIDS (0.9, Color.pink); 
        
        private final double scale;
        private final Color color;
 
        private State(double scale, Color color) {
            this.scale = scale;
            this.color = color;
        }

        private double getScale() {
            return scale;
        }
        
        private Color getColor() {
            return color;
        }
        
        private double getHexDrawScale() {
            return 1 - (1 - scale) / 2; 
        }
        
        private GeneralPath getInnerHexagon(GeneralPath hexagon, HexPoint center) {
            //inner hexagons are drawn outlined (not filled)
            //for this draw, the stroke width is half the scale reduction 
            //the scale factor is multiplied by the average of hex width / height in order
            //to get a good estimate for which for stroke width the hex borders are touched
            //by the stroke

            AffineTransform at =
                    AffineTransform.getScaleInstance(getHexDrawScale(), getHexDrawScale());
            GeneralPath innerHexagon = (GeneralPath) hexagon.createTransformedShape(at);

            // Translate innerHexagon to make it concentric.
            Rectangle2D innerBounds = innerHexagon.getBounds2D();
            HexPoint innerCenter = new HexPoint(
                    innerBounds.getX() + innerBounds.getWidth() / 2.0, 
                    innerBounds.getY() + innerBounds.getHeight() / 2.0
            );
            HexPoint difference = HexPoint.difference(center, innerCenter);
            
            at = AffineTransform.getTranslateInstance(difference.getX(), difference.getY());
            innerHexagon.transform(at);

            return innerHexagon;
        }
      

        private double getStrokeWidth(GeneralPath hexagon) {
            return ( 1 - getHexDrawScale() ) *
                    ( hexagon.getBounds().width + hexagon.getBounds().height ) / 2;
        }
    }
    
    /**
     * Static class for GUIHex Dimensions 
     */
    private static class Dimensions {
        private final double zoomFactor;
        private final double tokenDiameter;
        
        private final Map<HexSide, HexPoint> points;
        private final HexPoint center;
        
        private final GeneralPath hexagon;
        
        private final Rectangle rectBound;
        // The area which would have to be repainted if any hex marking is changed
        private final Rectangle marksDirtyRectBound;
        
        private Dimensions(HexMap hexMap, MapHex hex, double scale, double zoomFactor) {
            this.zoomFactor = zoomFactor;

            double cx = hexMap.calcXCoordinates(hex.getCoordinates().getCol(), hexMap.tileXOffset);
            double cy = hexMap.calcYCoordinates(hex.getCoordinates().getRow(), hexMap.tileYOffset);
            points = hexMap.getMapManager().getMapOrientation().setGUIVertices(cx, cy, scale);
            
            tokenDiameter = NORMAL_TOKEN_SIZE * zoomFactor;

            hexagon = makePolygon();

            center = HexPoint.middle(points.get(HexSide.defaultRotation()), 
                    points.get(HexSide.defaultRotation().opposite()));

            rectBound = hexagon.getBounds();
            marksDirtyRectBound = new Rectangle (
                    rectBound.x - marksDirtyMargin,
                    rectBound.y - marksDirtyMargin,
                    rectBound.width + marksDirtyMargin * 2,
                    rectBound.height + marksDirtyMargin * 2
            );
        }
        
        // Replace with Path2D
        private GeneralPath makePolygon() {
            GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 6);
            
            HexPoint start = points.get(HexSide.defaultRotation());
            polygon.moveTo((float) start.getX(), (float) start.getY());

            for (HexSide side:HexSide.allExceptDefault()) {
                HexPoint point = points.get(side);
                polygon.lineTo((float) point.getX(), (float) point.getY());
            }
            polygon.closePath();
            return polygon;
        }

  
    }
    
    // STATIC CONSTANTS
    private static final int NORMAL_TOKEN_SIZE = 15;
    private static final double TILE_GRID_SCALE = 14.0;
    private static final double CITY_SIZE = 16.0;

    private static final Color BAR_COLOUR = Color.BLUE;
    private static final int BAR_WIDTH = 5;

    private static final Color highlightedFillColor = new Color(255,255,255,128);
    private static final Color highlightedBorderColor = Color.BLACK;
    private static final Stroke highlightedBorderStroke = new BasicStroke(3);

    
    // Defines by how much the hex bounds have to be increased in each direction
    // for obtaining the dirty rectangle (markings could got beyond hex limits)
    private static final int marksDirtyMargin = 4;
    
    // positions of offStation Tokens
    private static final int[] offStationTokenX = new int[] { -11, 0 };
    private static final int[] offStationTokenY = new int[] { -19, 0 };

    // static fields
    private final HexMap hexMap;
    private final MapHex hex;
    
    // dynamic fields
    private Dimensions dimensions;

    private State state;
    
    private HexUpgrade upgrade;

    private List<HexSide> barSides;

    // A counter instead of a boolean is used here in order to be able to correctly
    // handle racing conditions for mouse events.
    private int highlightCounter = 0;

    public GUIHex(HexMap hexMap, MapHex hex, double scale) {
        this.hexMap = hexMap;
        this.hex = hex;
        hex.addObserver(this);
        this.setDimensions(scale, 1.0);
        this.state = State.NORMAL;
    }
    
    public void setDimensions(double scale, double zoomFactor) {
        dimensions = new Dimensions(hexMap, hex, scale, zoomFactor);
    }

    // TODO: rename to getModel()
    public MapHex getHex() {
        return this.hex;
    }

    public HexMap getHexMap() {
        return hexMap;
    }

    public Point2D getStopPoint2D(Stop stop){
        return getTokenCenter(0, stop).get2D();
    }

    public Point2D getSidePoint2D(HexSide side){
        HexPoint middle = HexPoint.middle(dimensions.points.get(side), 
                dimensions.points.get(side.next()));
        return middle.get2D();
    }

    public Point2D getCenterPoint2D() {
        return dimensions.center.get2D();
    }

    
    // TODO: Make this based on MapHex model
    public void addBar(HexSide side) {
        if (barSides == null) {
            barSides = Lists.newArrayListWithCapacity(2);
        }
        barSides.add(side);
    }

    public Rectangle getBounds() {
        return dimensions.rectBound;
    }
    
    public Rectangle getMarksDirtyBounds() {
        return dimensions.marksDirtyRectBound;
    }

    public boolean contains(Point2D.Double point) {
        return (dimensions.hexagon.contains(point));
    }

    public boolean contains(Point point) {
        return (dimensions.hexagon.contains(point));
    }

    public boolean intersects(Rectangle2D r) {
        return (dimensions.hexagon.intersects(r));
    }

    public void setState(State state) {
        if (this.state != state) {
            //trigger hexmap marks repaint if status changes
            hexMap.repaintMarks(getMarksDirtyBounds());
            hexMap.repaintTiles(getBounds()); // tile is drawn smaller if selected
        }
        this.state = state;
    }
    
    public State getState() {
        return state;
    }

    /**
     * Indicate that this hex should be highlighted
     */
    public void addHighlightRequest() {
        //trigger hexmap marks repaint if hex becomes highlighted
        if (highlightCounter == 0) hexMap.repaintMarks(getMarksDirtyBounds());

        highlightCounter++;
    }
    
    /**
     * Indicate that this hex does not need to be highlighted any more (from the
     * caller's point of view).
     * Note that the hex could still remain highlighted if another entity has requested
     * highlighting.
     */
    public void removeHighlightRequest() {
        highlightCounter--;
        //trigger hexmap marks repaint if hex becomes not highlighted
        if (highlightCounter == 0) hexMap.repaintMarks(getMarksDirtyBounds());
    }
    
    public boolean isHighlighted() {
        return (highlightCounter > 0);
    }
    
    public void setUpgrade(HexUpgrade upgrade) {
        this.upgrade = upgrade;
        hexMap.repaintTiles(getBounds());
        hexMap.repaintTokens(getBounds()); // needed if new tile has new token placement spot
    }
    
    public HexUpgrade getUpgrade() {
        return upgrade;
    }

    /**
     * @return the current tile shown on the map (if an upgrade is shown the upgrade target tile is returned)
     */
    private Tile getVisibleTile() {
        if (upgrade instanceof TileHexUpgrade) {
            return ((TileHexUpgrade)upgrade).getUpgrade().getTargetTile();
        } else {
            return hex.getCurrentTile();
        }
    }
    
    /**
     * @return the current tile rotation (if an upgrade is shown the rotation of that tile is returned)
     */
    private HexSide getVisibleRotation() {
        if (upgrade instanceof TileHexUpgrade) {
            return ((TileHexUpgrade)upgrade).getCurrentRotation();
        } else {
            return hex.getCurrentTileRotation();
        }
    }

    private boolean isTilePainted() {
        Tile visibleTile = getVisibleTile();
        return visibleTile != null && hexMap.isTilePainted(visibleTile); 
    }
    
    public void paintTile(Graphics2D g) {
        if (isTilePainted()) {
            GUIGlobals.setRenderingHints(g);
            paintOverlay(g);
        }
    }
    
    /**
     * Marks are selected / selectable / highlighted
     * @param g
     */
    public void paintMarks(Graphics2D g) {
        GUIGlobals.setRenderingHints(g);

        if (state != State.NORMAL) {
            Stroke oldStroke = g.getStroke();                
            g.setStroke(new BasicStroke((float) state.getStrokeWidth(dimensions.hexagon)));
            g.setColor(state.getColor());                
            g.draw(state.getInnerHexagon(dimensions.hexagon, dimensions.center));            
            g.setStroke(oldStroke);                
        }

        //highlight on top of tiles
        if (isHighlighted()) {
            g.setColor(highlightedFillColor);
            g.fill(dimensions.hexagon);
            Stroke oldStroke = g.getStroke();                
            g.setStroke(highlightedBorderStroke);
            g.setColor(highlightedBorderColor);
            g.draw(dimensions.hexagon);
            g.setStroke(oldStroke);
        }

    }
    
    public void paintTokensAndText(Graphics2D g) {

        GUIGlobals.setRenderingHints(g);

        paintStationTokens(g);
        paintOffStationTokens(g);

        if (!isTilePainted()) return;
        
        FontMetrics fontMetrics = g.getFontMetrics();
        if (getHex().getTileCost() > 0 ) {
            g.drawString(
                    Bank.format(getHex(), getHex().getTileCost()),
                    dimensions.rectBound.x
                            + (dimensions.rectBound.width - fontMetrics.stringWidth(Integer.toString(getHex().getTileCost())))
                            * 3 / 5,
                    dimensions.rectBound.y
                            + ((fontMetrics.getHeight() + dimensions.rectBound.height) * 9 / 15));
        }

        Map<PublicCompany, Stop> homes = getHex().getHomes();

        if (homes  != null) {
            for (PublicCompany company : homes.keySet()) {
                if (company.isClosed()) continue;

                // Only draw the company name if there isn't yet a token of that company
                if (hex.hasTokenOfCompany(company)) continue;
                // Do not draw if hex is never blocked for token lays 
                if (hex.getBlockedForTokenLays() == MapHex.BlockedToken.NEVER) continue;
                
                Stop homeCity = homes.get(company);
                if (homeCity.getRelatedStation() == null) { // not yet decided where the token will be
                    // find a free slot
                    Set<Stop> stops = getHex().getStops();
                    for (Stop stop:stops) {
                        if (stop.hasTokenSlotsLeft()) {
                            homeCity = stop;
                            break;
                        }
                    }
                }
                // check the number of tokens laid there already
                HexPoint p = getTokenCenter (1, homeCity);
                drawHome(g, company, p);
            }
        }

        if (hex.isBlockedByPrivateCompany()) {
           PrivateCompany p = hex.getBlockingPrivateCompany();
           String text = "(" + p.getId() + ")";
           g.drawString(
                   text,
                   dimensions.rectBound.x
                   + (dimensions.rectBound.width - fontMetrics.stringWidth(text))
                   * 1 / 2,
                   dimensions.rectBound.y
                   + ((fontMetrics.getHeight() + dimensions.rectBound.height) * 5 / 15));
        }

        if (hex.isReservedForCompany()
        		&& hex.isPreprintedTileCurrent()) {
        	String text = "[" + hex.getReservedForCompany().getId() + "]";
            g.drawString(
                  text,
                  dimensions.rectBound.x
                  + (dimensions.rectBound.width - fontMetrics.stringWidth(text))
                  * 1 / 2,
                  dimensions.rectBound.y
                  + ((fontMetrics.getHeight() + dimensions.rectBound.height) * 5 / 25));
        }

    }

    private void paintOverlay(Graphics2D g2) {
        Tile visibleTile = this.getVisibleTile();
        HexSide visibleRotation = this.getVisibleRotation();
        
        GUITile.paintTile(g2, dimensions.center, this, visibleTile, visibleRotation, 
                state.getScale(), hexMap.getZoomStep());
    }

    public void paintBars(Graphics2D g) {
        if (barSides == null) return;
        for (HexSide startPoint : barSides) {
            drawBar(g, dimensions.points.get(startPoint), dimensions.points.get(startPoint.next()));
        }
    }

    protected void drawBar(Graphics2D g2d, HexPoint start, HexPoint end) {
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();

        g2d.setColor(BAR_COLOUR);
        g2d.setStroke(new BasicStroke(BAR_WIDTH));
        g2d.draw(new Line2D.Double(start.get2D(),end.get2D()));

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }

    private void paintStationTokens(Graphics2D g2) {
        for (Stop stop:getHex().getStops()) {
            int j = 0;
            for (BaseToken token:stop.getBaseTokens()) {
                HexPoint origin = getTokenCenter(j++, stop);
                PublicCompany company = token.getParent();
                drawBaseToken(g2, company, origin, dimensions.tokenDiameter);
            }
            // check for temporary token
            if (upgrade instanceof TokenHexUpgrade && ((TokenHexUpgrade) upgrade).getAction() instanceof LayBaseToken) {
                TokenHexUpgrade tokenUpgrade = (TokenHexUpgrade) upgrade;
                HexPoint origin = getTokenCenter(j++, tokenUpgrade.getSelectedStop());
                PublicCompany company = tokenUpgrade.getAction().getCompany();
                drawBaseToken(g2, company, origin, dimensions.tokenDiameter);
            }
        }
    }

    // FIXME: Where to paint more than one offStationTokens?
    private void paintOffStationTokens(Graphics2D g2) {
        int i = 0;
        for (BonusToken token : hex.getBonusTokens())  {
            HexPoint origin = dimensions.center.translate(offStationTokenX[i], offStationTokenY[i]);
            drawBonusToken(g2, token, origin);
            if (++i > 1) return;
            
        }
        // check for temporary token
        if (upgrade instanceof TokenHexUpgrade && ((TokenHexUpgrade) upgrade).getAction() instanceof LayBonusToken) {
            HexPoint origin = dimensions.center.translate(offStationTokenX[i], offStationTokenY[i]);
            BonusToken token = ((LayBonusToken)((TokenHexUpgrade) upgrade).getAction()).getToken();
            drawBonusToken(g2, token, origin);
        }
    }

    private void drawBaseToken(Graphics2D g2, PublicCompany co, HexPoint center, double diameter) {

        GUIToken token = new GUIToken(
                co.getFgColour(), co.getBgColour(), co.getId(), center, diameter);
        // token.setBounds((int)Math.round(dimensions.center.getX()-0.5*diameter), (int) Math.round(dimensions.center.getY()-0.5*diameter),
        //        diameter, diameter);

        token.drawToken(g2);

    }

    private void drawHome (Graphics2D g2, PublicCompany co, HexPoint origin) {

        GUIToken.drawTokenText(co.getId(), g2, Color.BLACK, origin, dimensions.tokenDiameter);
    }

    private void drawBonusToken(Graphics2D g2, BonusToken bt, HexPoint origin) {
        GUIToken token =
                new GUIToken(Color.BLACK, Color.WHITE, "+" + bt.getValue(),
                        origin, 15);
        token.drawToken(g2);
    }

    private HexPoint getTokenCenter(int currentToken, Stop stop) {
        // Find the correct position on the tile
        int positionCode = stop.getRelatedStation().getPosition();
        
        HexPoint tokenCenter;
        if (positionCode != 0) {
            // FIXME: Check if we need both x and y 
            // or only y as in Rails1.x
            double initial = TILE_GRID_SCALE * dimensions.zoomFactor;
            double r = MapOrientation.DEG30 * (positionCode / 50);
            tokenCenter = new HexPoint(0, initial).rotate(r);
        } else {
            tokenCenter = new HexPoint(0.0, 0.0);
        }

        // Correct for the number of base slots and the token number
        double delta_x = 0, delta_y = 0;
        switch (stop.getSlots()) {
        case 2:
            delta_x = (-0.5 + currentToken) * CITY_SIZE * dimensions.zoomFactor;
            break;
        case 3:
            if (currentToken < 2) {
                delta_x = (-0.5 + currentToken) * CITY_SIZE * dimensions.zoomFactor;
                delta_y = -3 + 0.25 * MapOrientation.SQRT3 * CITY_SIZE * dimensions.zoomFactor;
            } else {
               delta_y = -(3 + 0.5 * CITY_SIZE * dimensions.zoomFactor);
            }
            break;
        case 4:
            delta_x = (-0.5 + currentToken % 2) * CITY_SIZE * dimensions.zoomFactor;
            delta_y = (0.5 - currentToken / 2) * CITY_SIZE * dimensions.zoomFactor;
            break;
        case 6:
            switch (currentToken)  {
            case 0:
                delta_x += (-1) * CITY_SIZE * dimensions.zoomFactor;
                delta_y += (-0.5) * CITY_SIZE * dimensions.zoomFactor;
                break;
            case 1:
                delta_x += (-1) * CITY_SIZE * dimensions.zoomFactor;
                delta_y += (0.5) * CITY_SIZE * dimensions.zoomFactor;
                break;
            case 2:
                delta_y += (1) * CITY_SIZE * dimensions.zoomFactor;
                break;
            case 3:
                delta_x += (1) * CITY_SIZE * dimensions.zoomFactor;
                delta_y += (0.5) * CITY_SIZE * dimensions.zoomFactor;
                break;
            case 4:
                delta_x += (1) * CITY_SIZE * dimensions.zoomFactor;
                delta_y += (-0.5) * CITY_SIZE * dimensions.zoomFactor;
                break;
            case 5:
                delta_y += (-1) * CITY_SIZE * dimensions.zoomFactor;
                break;
            }

        }
        tokenCenter = tokenCenter.translate(delta_x, delta_y);

        // Correct for the tile base and actual rotations
        HexSide rotation = hex.getCurrentTileRotation();

        double radians = MapOrientation.rotationInRadians(hex, rotation);
        tokenCenter = tokenCenter.rotate(radians);
        
        tokenCenter = dimensions.center.translate(tokenCenter.getX(), - tokenCenter.getY());
        return tokenCenter;
    }

    public String getToolTip() {
        if (upgrade != null)
            return upgrade.getUpgradeToolTip();
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
        Tile currentTile = hex.getCurrentTile();
        
        StringBuffer tt = new StringBuffer("<html>");
        tt.append("<b>Hex</b>: ").append(hex.toText());
        // For debugging: display x,y-coordinates
        //tt.append("<small> x=" + x + " y="+y+"</small>");
        
        tt.append("<br><b>Tile</b>: ").append(currentTile.toText());
        
        // For debugging: display rotation
        //tt.append("<small> rot=" + currentTileOrientation + "</small>");

        if (hex.hasValuesPerPhase()) {
            tt.append("<br>Value ");
            tt.append(hex.getCurrentValueForPhase(hexMap.getPhase())).append(" [");
            List<Integer> values = hex.getValuesPerPhase();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) tt.append(",");
                tt.append(values.get(i));
            }
            tt.append("]");
        } else if (currentTile.hasStations()) {
            for (Stop stop : hex.getStops()) {
                Station station = stop.getRelatedStation();
                tt.append("<br>  ").append(station.toText())
                    .append(" (").append(hex.getConnectionString(station))
                    .append("): value ");
                tt.append(station.getValue());
                if (station.getBaseSlots() > 0) {
                    tt.append(", ").append(station.getBaseSlots()).append(" slots");
                    Set<BaseToken> tokens = stop.getBaseTokens();
                    if (tokens.size() > 0) {
                        tt.append(" (");
                        int oldsize = tt.length();
                        for (BaseToken token : tokens) {
                            if (tt.length() > oldsize) tt.append(",");
                            tt.append(token.getParent().getId());
                        }
                        tt.append(")");
                    }
                }
                // TEMPORARY
                tt.append(" <small>pos=" + station.getPosition() + "</small>");
            }
            tt.append(bonusToolTipText(currentTile.getRevenueBonuses()));
        }

        // revenueBonuses
        tt.append(bonusToolTipText(hex.getRevenueBonuses()));

        String upgrades = currentTile.getUpgradesString(hex);
        if (upgrades.equals("")) {
            tt.append("<br>No upgrades");
        } else {
            tt.append("<br><b>Upgrades</b>: ").append(upgrades);
            if (hex.getTileCost() > 0)
                tt.append("<br>Upgrade cost: "
                          + Bank.format(hex, hex.getTileCost()));
        }

        if (getHex().getDestinations() != null) {
            tt.append("<br><b>Destination</b>:");
            for (PublicCompany dest : getHex().getDestinations()) {
                tt.append(" ");
                tt.append(dest.getId());
            }
        }


        tt.append("</html>");
        return tt.toString();
    }
    
    public void update() {
        hexMap.repaintTiles(getBounds());
        hexMap.repaintTokens(getBounds()); // needed if new tile has new token placement spot
    }
    
    public String toText() {
        return hex.toText();
    }


    // Observer methods
    @Override
    public void update(String text) {
        update();
    }
    
    @Override
    public Observable getObservable() {
        return hex;
    }

    // Object methods
    @Override
    public String toString () {
        return toText() + " (" + hex.getCurrentTile().toText() + ")";
    }

}
