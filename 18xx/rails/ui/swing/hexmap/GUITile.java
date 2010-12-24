/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/GUITile.java,v 1.25 2010/05/15 19:05:39 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.ImageLoader;

/**
 * This class represents the GUI version of a tile.
 */
public class GUITile {

    protected int tileId;

    protected TileI tile = null;
    protected String tileType = null;

    protected int picId;
    protected BufferedImage tileImage = null;

    protected int rotation = 0;

    protected double tileScale = GUIHex.NORMAL_SCALE;

    protected double baseRotation;

    protected MapHex hex = null;
    protected GUIHex guiHex = null;

    protected static ImageLoader imageLoader = GameUIManager.getImageLoader();

    protected AffineTransform af = new AffineTransform();

    public static final double DEG60 = Math.PI / 3;

    public static final double SVG_X_CENTER_LOC = 0.489;
    public static final double SVG_Y_CENTER_LOC = 0.426;

    protected static Logger log =
            Logger.getLogger(GUITile.class.getPackage().getName());

    public GUITile(int tileId, GUIHex guiHex) {
        this.guiHex = guiHex;
        this.tileId = tileId;
        this.hex = (MapHex)guiHex.getModel();
        TileManager tileManager = guiHex.getHexMap().orUIManager.getTileManager();
        tile = tileManager.getTile(tileId);
        picId = tile.getPictureId();

        if (hex.getTileOrientation() == MapHex.EW) {
            baseRotation = 0.5 * DEG60;
        } else {
            baseRotation = 0.0;
        }
    }

    public void setRotation(int rotation) {
        this.rotation = rotation % 6;
    }

    /**
     * Rotate right (clockwise) until a valid orientation is found.
     *
     * @param initial: First rotation to try. Should be 0 for the initial tile
     * drop, and 1 at subsequent rotation attempts.
     * @return <b>false</b> if no valid rotation exists (i.e. the tile cannot
     * be laid).
     */
    public boolean rotate(int initial, GUITile previousGUITile,
            boolean mustConnect) {
        int i, j, k, l, tempRot, tempTileSide, prevTileSide;
        TileI prevTile = previousGUITile.getTile();
        int prevTileRotation = previousGUITile.getRotation();
        MapHex nHex;

        boolean connected;

        int fixedRotation = getTile().getFixedOrientation();
        if (fixedRotation >= 0) {
            setRotation (fixedRotation);
            return true;
        }

        /* Loop through all possible rotations */
        rot: for (i = initial; i < 6; i++) {
            connected = !mustConnect;
            tempRot = (rotation + i) % 6;
            Map<Integer, Integer> oldCities = new HashMap<Integer, Integer>(4);
            Map<Integer, Integer> newCities = new HashMap<Integer, Integer>(4);

            /* Loop through all hex sides */
            for (j = 0; j < 6; j++) {
                tempTileSide = (6 + j - tempRot) % 6;
                prevTileSide = (6 + j - prevTileRotation) % 6;

                if (tile.hasTracks(tempTileSide)) {
                    // If the tile has tracks against that side, but there is no
                    // neighbour, forbid this rotation.
                    if (!hex.hasNeighbour(j) && !hex.isOpenSide(j)) {
                        continue rot;
                    }
                    // If the tile must be connected (i.e. not laid on the
                    // operating company home hex, and not a special tile lay),
                    // at least one neighbour must have a track against
                    // a side of this tile that also has a track.
                    if (mustConnect) {
                        nHex = hex.getNeighbor(j);
                        if (nHex != null && nHex.getCurrentTile().hasTracks(
                                j + 3 - nHex.getCurrentTileRotation())) {
                            connected = true;
                        }
                    }
                    // If the previous tile has tracks against this side too,
                    // these must all be preserved.
                    if (prevTile.hasTracks(prevTileSide)) {
                        List<Track> newTracks =
                                tile.getTracksPerSide(tempTileSide);
                        old: for (Track oldTrack : prevTile.getTracksPerSide(prevTileSide)) {
                            if (oldTrack.getEndPoint(prevTileSide) >= 0) {
                                // Old track ending in another side
                                for (Track newTrack : newTracks) {
                                    if ((tempRot + newTrack.getEndPoint(tempTileSide)) % 6 == (prevTileRotation + oldTrack.getEndPoint(prevTileSide)) % 6) {
                                        // OK, this old track is preserved
                                        continue old;
                                    }
                                }
                                // Found an unpreserved track - stop checking
                                continue rot;
                            } else {
                                // Old track ending in a station
                                // All old tracks ending the same/different
                                // stations must keep doing so (except when
                                // downgrading to plain track, as in 1856)
                                if (tile.hasStations()) {
                                    //log.debug("[" + i + "," + j + "] Found "
                                    //     + oldTrack.getEndPoint(prevTileSide));
                                    oldCities.put(prevTileSide,
                                        oldTrack.getEndPoint(prevTileSide));
                                } else {
                                    // Downgraded
                                    // Assume there are only two exits
                                    // (this is the only known case for downgrading:
                                    // #3->#7, #4->#9, #58->#8).
                                    // Find the other new exit
                                    int otherNewEndPoint = newTracks.get(0).getEndPoint(tempTileSide);
                                    // Calculate the corresponding old tile side number
                                    int otherOldEndPoint = (otherNewEndPoint + tempRot - prevTileRotation + 6) % 6;
                                               // That old tile side must have track too
                                    if (prevTile.getTracksPerSide(otherOldEndPoint) == null
                                            || prevTile.getTracksPerSide(otherOldEndPoint).isEmpty()) {
                                        continue rot;
                                    }
                                    //log.debug("[" + i + "," + j + "] Downgraded");
                                }
                            }
                        }
                    }

                    // If the previous tile has tracks against that side, but
                    // the new one has not, forbid this rotation (not preserving
                    // existing track).
                } else {
                    if (prevTile.hasTracks(prevTileSide)) {
                        continue rot;
                    }
                    // TODO: Add a check for preserving station connections
                    // on multi-station tiles.
                }
            }

            // Finish the city connection check
            if (!oldCities.isEmpty()) {
                int endPoint, kk, ll, kkk, lll;
                for (k = 0; k < 6; k++) {
                    tempTileSide = (6 + k - tempRot) % 6;
                    for (Track newTrack : tile.getTracksPerSide(tempTileSide)) {
                        endPoint = newTrack.getEndPoint(tempTileSide);
                        if (endPoint < 0)
                            newCities.put(tempTileSide, endPoint);
                    }
                }
                // For each pair of old city connections ending in
                // the same/different cities, a similar new pair must exist
                for (k = 0; k < 5; k++) {
                    kk = (6 + k - tempRot) % 6;
                    kkk = (6 + k - prevTileRotation) % 6;
                    if (oldCities.get(kkk) == null) continue;
                    for (l = k + 1; l < 6; l++) {
                        ll = (6 + l - tempRot) % 6;
                        lll = (6 + l - prevTileRotation) % 6;
                        if (oldCities.get(lll) == null) continue;
                        // If new tile is missing a connection, skip
                        //log.debug("Found " + oldCities.get(kkk) + " & "
                        //          + oldCities.get(lll));
                        //log.debug("Check " + newCities.get(kkk) + " & "
                        //          + newCities.get(ll));
                        if (newCities.get(kk) == null
                            || newCities.get(ll) == null) continue rot;
                        // If connected cities do not correspond, skip
                        // (this is the "OO brown upgrade get-right" feature)
                        // Only apply this check if the number of cities has not decreased
                        if (getTile().getNumStations() < prevTile.getNumStations()) continue;
                        //log.debug("Compare "+oldCities.get(kkk)+"/"+oldCities.get(lll)
                        //        +" ~ "+newCities.get(kk)+"/"+newCities.get(ll));
                        if ((oldCities.get(kkk).equals(oldCities.get(lll)))
                                != (newCities.get(kk).equals(newCities.get(ll)))) {
                            log.debug("No match!");
                            continue rot;
                        }
                    }
                }

            }

            if (j == 6 && connected) {
                /*
                 * If we have successfully checked all hex sides, we have found
                 * a valid rotation, so stop here.
                 */
                setRotation(tempRot);
                return true;
            }
        }
        if (i == 6) {
            /*
             * If we have rotated six times, no valid rotation has been found.
             */
            return false;
        } else {
            return true;
        }
    }

    public int getRotation() {
        return rotation;
    }

    public void setScale(double scale) {
        tileScale = scale;
    }

    public void paintTile(Graphics2D g2, int x, int y) {

        int zoomStep = guiHex.getHexMap().getZoomStep();

        tileImage = imageLoader.getTile(picId, zoomStep);

        if (tileImage != null) {

            double radians = baseRotation + rotation * DEG60;
            int xCenter = (int) Math.round(tileImage.getWidth()
                    * SVG_X_CENTER_LOC * tileScale);
            int yCenter = (int) Math.round(tileImage.getHeight()
                    * SVG_Y_CENTER_LOC * tileScale);

            af = AffineTransform.getRotateInstance(radians, xCenter, yCenter);
            af.scale(tileScale, tileScale);

            RenderingHints rh = new RenderingHints(null);
            rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            rh.put(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            rh.put(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            rh.put(RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_DISABLE);
            rh.put(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            rh.put(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            rh.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            AffineTransformOp aop = new AffineTransformOp(af, rh);

            g2.drawImage(tileImage, aop, x - xCenter, y - yCenter);
        } else {
        	log.error("No image for tile "+tileId+" on hex "+guiHex.getName());
        }
    }

    public TileI getTile() {
        return tile;
    }

    public int getTileId() {
        return tileId;
    }

}
