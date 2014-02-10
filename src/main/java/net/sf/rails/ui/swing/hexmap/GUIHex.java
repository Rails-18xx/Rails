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

import net.sf.rails.algorithms.RevenueBonusTemplate;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.Currency;
import net.sf.rails.game.HexSide;
import net.sf.rails.game.TileHexUpgrade;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapOrientation;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Station;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Tile;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;
import net.sf.rails.ui.swing.GUIGlobals;
import net.sf.rails.ui.swing.GUIToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;



/**
 * Base class that holds common components for GUIHexes of all orientations.
 */

public class GUIHex implements Observer {

    private static final Logger log =
            LoggerFactory.getLogger(GUIHex.class);
    
    public static final double NORMAL_SCALE = 1.0;
    private static final double SELECTABLE_SCALE = 0.9;
    private static final double SELECTED_SCALE = 0.8;

    private static final int NORMAL_TOKEN_SIZE = 15;
    private static final double TILE_GRID_SCALE = 14.0;
    private static final double CITY_SIZE = 16.0;

    private static final Color BAR_COLOUR = Color.BLUE;
    private static final int BAR_WIDTH = 5;

    private static final Color selectedColor = Color.red;
    private static final Color selectableColor = Color.red;
    private static final Color highlightedFillColor = new Color(255,255,255,128);
    private static final Color highlightedBorderColor = Color.BLACK;
    private static final Stroke highlightedBorderStroke = new BasicStroke(3);

    /**
     * Defines by how much the hex bounds have to be increased in each direction
     * for obtaining the dirty rectangle (markings could got beyond hex limits)
     */
    private static final int marksDirtyMargin = 4;
    
    /**
     * Class that describes x-y coordinates for GUIHexes
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

    // Fields
    private final HexMap hexMap; // Containing this hex
    private final MapHex hex;

    private final Map<HexSide, HexPoint> points = 
            Maps.newHashMapWithExpectedSize(6);

    private GUITile currentGUITile = null;
    private GUITile provisionalGUITile = null;
    private TileHexUpgrade upgrade;

    private GeneralPath innerHexagonSelected;
    private double selectedStrokeWidth;
    private GeneralPath innerHexagonSelectable;
    private double selectableStrokeWidth;
    private HexPoint center;
    private double zoomFactor = 1.0;
    private double tokenDiameter = NORMAL_TOKEN_SIZE;

    private GeneralPath hexagon;
    private Rectangle rectBound;
    private List<HexSide> barSides;
    private String toolTip;

    /**
     * The area which would have to be repainted if any hex marking is changed
     */
    private Rectangle marksDirtyRectBound;

    private boolean selected;
    private boolean selectable;
    /**
     * A counter instead of a boolean is used here in order to be able to correctly
     * handle racing conditions for mouse events.
     */
    private int highlightCounter = 0;

    private GUIHex(HexMap hexMap, MapHex hex) {
        this.hexMap = hexMap;
        this.hex = hex;
        hex.addObserver(this);
        initFromHexModel();
    }
    
    public static GUIHex create(HexMap hexMap, MapHex hex, double scale) {
        GUIHex guiHex = new GUIHex(hexMap, hex);
        guiHex.scaleHex(scale, 1.0);
        return guiHex;
    }

    public void scaleHex (double scale, double zoomFactor) {
        this.zoomFactor = zoomFactor;

        double cx = hexMap.calcXCoordinates(hex.getCoordinates().getCol(), hexMap.tileXOffset);
        double cy = hexMap.calcYCoordinates(hex.getCoordinates().getRow(), hexMap.tileYOffset);
        hexMap.getMapManager().getMapOrientation().setGUIVertices(points, cx, cy, scale);
        
        tokenDiameter = NORMAL_TOKEN_SIZE * zoomFactor;

        hexagon = makePolygon();
        setBounds(hexagon.getBounds());

        center = HexPoint.middle(points.get(HexSide.defaultRotation()), 
                points.get(HexSide.defaultRotation().opposite()));
        
        //inner hexagons are drawn outlined (not filled)
        //for this draw, the stroke width is half the scale reduction 
        //the scale factor is multiplied by the average of hex width / height in order
        //to get a good estimate for which for stroke width the hex borders are touched
        //by the stroke
        double hexDrawScale = 1 - (1 - SELECTED_SCALE) / 2; 
        innerHexagonSelected = defineInnerHexagon(hexDrawScale);
        selectedStrokeWidth = (float) ( 1 - hexDrawScale ) *
                ( hexagon.getBounds().width + hexagon.getBounds().height ) / 2;
        
        hexDrawScale = 1 - (1 - SELECTABLE_SCALE) / 2; 
        innerHexagonSelectable = defineInnerHexagon(hexDrawScale);
        selectableStrokeWidth = (float) ( 1 - hexDrawScale ) *
                ( hexagon.getBounds().width + hexagon.getBounds().height ) / 2;
    }

    private GeneralPath defineInnerHexagon(double innerScale) {

        AffineTransform at =
                AffineTransform.getScaleInstance(innerScale, innerScale);
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
        HexPoint middle = HexPoint.middle(
                points.get(side), points.get(side.next()));
        return middle.get2D();
    }

    public Point2D getCenterPoint2D() {
        return center.get2D();
    }

    private void initFromHexModel() {
        currentGUITile = new GUITile(hex.getCurrentTile(), this);
        currentGUITile.setRotation(hex.getCurrentTileRotation());
        toolTip = null;
    }

    public void addBar(HexSide side) {
        if (barSides == null) {
            barSides = Lists.newArrayListWithCapacity(2);
        }
        barSides.add(side);
    }

    public Rectangle getBounds() {
        return rectBound;
    }
    
    public Rectangle getMarksDirtyBounds() {
        return marksDirtyRectBound;
    }

    public void setBounds(Rectangle rectBound) {
        this.rectBound = rectBound;
        marksDirtyRectBound = new Rectangle (
                rectBound.x - marksDirtyMargin,
                rectBound.y - marksDirtyMargin,
                rectBound.width + marksDirtyMargin * 2,
                rectBound.height + marksDirtyMargin * 2
                );
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
        //trigger hexmap marks repaint if selected-status changes
        if (this.selected != selected) {
            hexMap.repaintMarks(getMarksDirtyBounds());
            hexMap.repaintTiles(getBounds()); // tile is drawn smaller if selected
        }

        this.selected = selected;
        if (selected) {
            currentGUITile.setScale(SELECTED_SCALE);
        } else {
            currentGUITile.setScale(isSelectable() ? SELECTABLE_SCALE : NORMAL_SCALE);
            provisionalGUITile = null;
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelectable(boolean selectable) {
        //trigger hexmap repaint if selectable-status changes
        if (this.selectable != selectable) {
            hexMap.repaintMarks(getMarksDirtyBounds());
            hexMap.repaintTiles(getBounds()); // tile is drawn smaller if selectable
        }

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

    private boolean isTilePainted() {
        return provisionalGUITile != null && hexMap.isTilePainted(provisionalGUITile.getTile()) 
                || currentGUITile != null && hexMap.isTilePainted(currentGUITile.getTile());
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

        if (isSelected()) {
            Stroke oldStroke = g.getStroke();                
            g.setStroke(new BasicStroke((float) selectedStrokeWidth));
            g.setColor(selectedColor);                
            g.draw(innerHexagonSelected);            
            g.setStroke(oldStroke);                
        } else if (isSelectable()) {
            Stroke oldStroke = g.getStroke();                
            g.setStroke(new BasicStroke((float) selectableStrokeWidth));
            g.setColor(selectableColor);
            g.draw(innerHexagonSelectable);            
            g.setStroke(oldStroke);                
        }

        //highlight on top of tiles
        if (isHighlighted()) {
            g.setColor(highlightedFillColor);
            g.fill(hexagon);
            Stroke oldStroke = g.getStroke();                
            g.setStroke(highlightedBorderStroke);
            g.setColor(highlightedBorderColor);
            g.draw(hexagon);
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
                    Currency.format(getHex(), getHex().getTileCost()),
                    rectBound.x
                            + (rectBound.width - fontMetrics.stringWidth(Integer.toString(getHex().getTileCost())))
                            * 3 / 5,
                    rectBound.y
                            + ((fontMetrics.getHeight() + rectBound.height) * 9 / 15));
        }

        Map<PublicCompany, Stop> homes = getHex().getHomes();

        if (homes  != null) {
            for (PublicCompany company : homes.keySet()) {
                if (company.isClosed()) continue;

                // Only draw the company name if there isn't yet a token of that company
                if (hex.hasTokenOfCompany(company)) continue;
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

        if (getHex().isBlockedForTileLays()) {
            List<PrivateCompany> privates =
                    //GameManager.getInstance().getCompanyManager().getAllPrivateCompanies();
            		hexMap.getOrUIManager().getGameUIManager().getRoot()
            			.getCompanyManager().getAllPrivateCompanies();
            for (PrivateCompany p : privates) {
                List<MapHex> blocked = p.getBlockedHexes();
                if (blocked != null) {
                    for (MapHex hex : blocked) {
                        if (getHex().equals(hex)) {
                        	String text = "(" + p.getId() + ")";
                            g.drawString(
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

        if (hex.isReservedForCompany()
        		&& hex.isPreprintedTileCurrent()) {
        	String text = "[" + hex.getReservedForCompany() + "]";
            g.drawString(
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
            if (hexMap.isTilePainted(provisionalGUITile.getTile())) {
                provisionalGUITile.paintTile(g2, center);
            }
        } else {
            if (hexMap.isTilePainted(currentGUITile.getTile())) {
                currentGUITile.paintTile(g2, center);
            }
        }
    }

    public void paintBars(Graphics2D g) {
        if (barSides == null) return;
        for (HexSide startPoint : barSides) {
            drawBar(g, points.get(startPoint), points.get(startPoint.next()));
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
            log.debug("Stop = " + stop + ",BaseTokens = " + stop.getBaseTokens());
            for (BaseToken token:stop.getBaseTokens()) {
                HexPoint origin = getTokenCenter(j++, stop);
                PublicCompany company = token.getParent();
                log.debug("Paint token of " + company + " on " + stop);
                drawBaseToken(g2, company, origin, tokenDiameter);
            }
        }
    }

    private static int[] offStationTokenX = new int[] { -11, 0 };
    private static int[] offStationTokenY = new int[] { -19, 0 };

    // FIXME: Where to paint more than one offStationTokens?
    private void paintOffStationTokens(Graphics2D g2) {
        int i = 0;
        for (BaseToken token : hex.getOffStationTokens()) {
            HexPoint origin = center.translate(offStationTokenX[i], offStationTokenY[i]);
                PublicCompany co = token.getParent();
                drawBaseToken(g2, co, origin, tokenDiameter);
            if (++i > 1) return;
        }
        
        for (BonusToken token : hex.getBonusTokens())  {
            HexPoint origin = center.translate(offStationTokenX[i], offStationTokenY[i]);
            drawBonusToken(g2, token, origin);
            if (++i > 1) return;
            
        }
    }

    private void drawBaseToken(Graphics2D g2, PublicCompany co, HexPoint center, double diameter) {

        GUIToken token = new GUIToken(
                co.getFgColour(), co.getBgColour(), co.getId(), center, diameter);
        // token.setBounds((int)Math.round(center.getX()-0.5*diameter), (int) Math.round(center.getY()-0.5*diameter),
        //        diameter, diameter);

        token.drawToken(g2);

    }

    private void drawHome (Graphics2D g2, PublicCompany co, HexPoint origin) {

        GUIToken.drawTokenText(co.getId(), g2, Color.BLACK, origin, tokenDiameter);
    }

    private void drawBonusToken(Graphics2D g2, BonusToken bt, HexPoint origin) {
        GUIToken token =
                new GUIToken(Color.BLACK, Color.WHITE, "+" + bt.getValue(),
                        origin, 15);
        token.drawToken(g2);
    }

    public TileHexUpgrade getUpgrade() {
        return upgrade;
    }
    
    public void rotateTile() {
        if (provisionalGUITile != null) {
            provisionalGUITile.rotate(upgrade, currentGUITile);
        }
        hexMap.repaintTiles(getBounds()); // provisional tile part of the tiles layer
    }

    public void forcedRotateTile() {
        if (provisionalGUITile != null) {
            provisionalGUITile.setRotation(provisionalGUITile.getRotation().next());
        }
        hexMap.repaintTiles(getBounds()); // provisional tile resides in tile layer
    }

    private HexPoint getTokenCenter(int currentToken, Stop stop) {
        // Find the correct position on the tile
        int positionCode = stop.getRelatedStation().getPosition();
        
        HexPoint tokenCenter;
        if (positionCode != 0) {
            // FIXME: Check if we need both x and y 
            // or only y as in Rails1.x
            double initial = TILE_GRID_SCALE * zoomFactor;
            double r = MapOrientation.DEG30 * (positionCode / 50);
            tokenCenter = new HexPoint(0, initial).rotate(r);
        } else {
            tokenCenter = new HexPoint(0.0, 0.0);
        }

        // Correct for the number of base slots and the token number
        double delta_x = 0, delta_y = 0;
        switch (stop.getSlots()) {
        case 2:
            delta_x = (-0.5 + currentToken) * CITY_SIZE * zoomFactor;
            break;
        case 3:
            if (currentToken < 2) {
                delta_x = (-0.5 + currentToken) * CITY_SIZE * zoomFactor;
                delta_y = -3 + 0.25 * MapOrientation.SQRT3 * CITY_SIZE * zoomFactor;
            } else {
               delta_y = -(3 + 0.5 * CITY_SIZE * zoomFactor);
            }
            break;
        case 4:
            delta_x = (-0.5 + currentToken % 2) * CITY_SIZE * zoomFactor;
            delta_y = (0.5 - currentToken / 2) * CITY_SIZE * zoomFactor;
        }
        tokenCenter = tokenCenter.translate(delta_x, delta_y);

        // Correct for the tile base and actual rotations
        HexSide rotation = hex.getCurrentTileRotation();

        double radians = MapOrientation.rotationInRadians(hexMap, rotation);
        tokenCenter = tokenCenter.rotate(radians);
        
        tokenCenter = center.translate(tokenCenter.getX(), - tokenCenter.getY());
        log.debug("Token Center p=" + tokenCenter + " for currentToken=" + currentToken);
        return tokenCenter;
    }

    public String toText() {
        return hex.toText();
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
                            tt.append(token.getId());
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
                          + Currency.format(hex, hex.getTileCost()));
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

    // FIXME: Can this still return false?
    public boolean dropTile(TileHexUpgrade upgrade) {
        this.upgrade = upgrade;

        provisionalGUITile = new GUITile(upgrade.getUpgrade().getTargetTile(),this);
        provisionalGUITile.setRotation(upgrade.getRotations().getNext(HexSide.defaultRotation()));
        if (provisionalGUITile != null) {
            provisionalGUITile.setScale(SELECTED_SCALE);
            toolTip = "Click to rotate";
            hexMap.repaintMarks(getBounds());
            hexMap.repaintTiles(getBounds()); // provisional tile resides in tile layer
        }
        return (provisionalGUITile != null);
    }

    /** forces the tile to drop */
    public void forcedDropTile(TileHexUpgrade upgrade, HexSide orientation) {
        provisionalGUITile = new GUITile(upgrade.getUpgrade().getTargetTile(), this);
        provisionalGUITile.setRotation(orientation);
        provisionalGUITile.setScale(SELECTED_SCALE);
        toolTip = "Click to rotate";
        hexMap.repaintTiles(getBounds()); // provisional tile resides in tile layer
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

    public HexSide getProvisionalTileRotation() {
        return provisionalGUITile.getRotation();
    }

    public void fixTile() {

        setSelected(false);
        toolTip = null;
    }

    public void removeToken() {
        setSelected(false);
        toolTip = null;
        hexMap.repaintTokens(getBounds());
    }

    public void fixToken() {
        setSelected(false);
        toolTip = null;
    }

    
    public String toString () {
        return toText() + " (" + hex.getCurrentTile().toText() + ")";
    }

    // Observer methods
    public void update(String text) {
        initFromHexModel();
        hexMap.repaintTiles(getBounds());
        hexMap.repaintTokens(getBounds()); // needed if new tile has new token placement spot
        provisionalGUITile = null;
        log.debug("GUIHex " + hex.toText() + " updated");
}
    
    public Observable getObservable() {
        return hex;
    }
}
