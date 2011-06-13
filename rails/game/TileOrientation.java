package rails.game;

/**
 * Tile orientation enumeration.
 * 
 * Tile orientation refers to "flat edges" parallel with coordinates system
 * axis. Thus there are two orientations: North-South
 * ({@link TileOrientation#NS NS}) and East-West
 * ({@link TileOrientation#EW EW}).
 * 
 * Although it seems neither is dominating in 18xx games North-South is used by
 * default for management and classification. So North-South orientation is
 * treated here as the natural one.
 * 
 * @author Adam Badura
 * @since 1.4.3
 */
public enum TileOrientation {
    /**
     * North-South tile orientation.
     * 
     * <p>This is default orientation for internal uses (which includes SVG
     * images).</p>
     */
    NS,

    /**
     * East-West tile orientation.
     */
    EW;


    /**
     * Returns rotation to be applied to {@link TileOrientation#NS}-oriented
     * tile to achieve this orientation.
     * 
     * <p>The rotation has to be done around center point of the tile.</p>
     * 
     * <p>This function returns {@literal 0} for {@link TileOrientation#NS}
     * since {@code NS}-oriented tile does not need any rotation to be
     * transformed into {@code NS}-oriented tile.</p>
     * 
     * @return Rotation to be applied to {@link TileOrientation#NS}-oriented
     *         tile to achieve this orientation.
     */
    public int getBaseRotation() {
        switch(this) {
        case NS:
            return 0;
        case EW:
            return 30;
        default:
            // Unexpected default.
            throw new AssertionError(this);
        }
    }
}
