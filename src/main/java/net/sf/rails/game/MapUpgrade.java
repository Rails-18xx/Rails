package net.sf.rails.game;

import java.util.Comparator;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/** MapUpgrade is an abstract class used for objects that represent possible upgrades to the map
 * Upgrades always have a target location.
 */
public abstract class MapUpgrade implements Comparable<MapUpgrade> {
    
    private boolean visible = true;
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }

    public abstract boolean isValid();
    
    public abstract Location getLocation();
    
    
    /**
     * @return integer used for sorting (lows first)
     */
    public abstract int getCompareId();
    
    public abstract Comparator<MapUpgrade> getComparator();

    @Override
    public int compareTo(MapUpgrade other) {
        return ComparisonChain.start()
                .compare(this.getCompareId(), other.getCompareId())
                .compare(other.isValid(), this.isValid())
                .compare(this, other, this.getComparator())
                .result();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("visible", visible)
                .add("isValid", isValid())
                .toString()
        ;
    }
    
}
