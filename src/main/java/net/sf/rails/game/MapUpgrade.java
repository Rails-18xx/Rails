package net.sf.rails.game;

/** MapUpgrade is an abstract class used for objects that represent possible upgrades to the map
 * Upgrades always have a target location.
 */
public abstract class MapUpgrade {
    
    private boolean visible = true;
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }

    public abstract boolean isValid();
    
    public abstract Location getLocation();
}
