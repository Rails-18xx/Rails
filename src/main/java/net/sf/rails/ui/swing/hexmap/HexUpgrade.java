package net.sf.rails.ui.swing.hexmap;

import java.util.Comparator;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/** 
 * HexUpgrade is an abstract class used for objects that represent possible upgrades to hexes.
 */
public abstract class HexUpgrade implements Comparable<HexUpgrade> {
    
    public static interface Invalids {};
    
    protected final GUIHex hex;

    private boolean visible = true;
    
    protected HexUpgrade(GUIHex hex) {
        this.hex = hex;
    }
    
    public GUIHex getHex() {
        return hex;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }

    public abstract boolean hasSingleSelection();
    
    public abstract void firstSelection();
    
    public abstract void nextSelection();
    
    public abstract Set<Invalids> getInvalids();

    public abstract boolean isValid();
    
    public abstract String getToolTip();
    
    /**
     * @return integer used for sorting (lows first)
     */
    public abstract int getCompareId();
    
    public abstract Comparator<HexUpgrade> getComparator();

    @Override
    public int compareTo(HexUpgrade other) {
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
