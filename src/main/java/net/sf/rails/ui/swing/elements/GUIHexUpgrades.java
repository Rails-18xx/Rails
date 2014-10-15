package net.sf.rails.ui.swing.elements;

import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SetMultimap;

/**
 * Class that wraps a Multimap between GuiHex and MapUpgrades
 */
public class GUIHexUpgrades {

    private ImmutableSetMultimap.Builder<GUIHex, HexUpgrade> allBuilder;

    private SetMultimap<GUIHex, HexUpgrade> allUpgrades;
    
    private GUIHex activeHex;
    
    private NavigableSet<HexUpgrade> validUpgrades;
    
    private SortedSet<HexUpgrade> invalidUpgrades;
    
    private SortedSet<HexUpgrade> invisibleUpgrades;
   
    private HexUpgrade activeUpgrade;
    
    
    private GUIHexUpgrades() {
        clear(); // used to initialize
    }
    
    public static GUIHexUpgrades create() {
        return new GUIHexUpgrades();
    }
    
    public void put(GUIHex hex, HexUpgrade upgrade) {
        allBuilder.put(hex, upgrade);
    }

    public void putAll(GUIHex hex, Set<? extends HexUpgrade> upgrades) {
        for (HexUpgrade upgrade:upgrades) {
            allBuilder.put(hex, upgrade);
        }
    }
    
    public void build() {
        allUpgrades = allBuilder.build();
        allBuilder = ImmutableSetMultimap.builder();
    }
    
    public boolean contains(GUIHex hex) {
        return allUpgrades.containsKey(hex);
    }
    
    public Set<GUIHex> getHexes() {
        return allUpgrades.keySet();
    }
    
    public boolean hasElements() {
        return !allUpgrades.isEmpty();
    }
    
    public Set<HexUpgrade> getUpgrades(GUIHex hex) {
        return allUpgrades.get(hex);
    }
    
    public void setActiveHex(GUIHex hex) {
        activeHex = hex;
        if (hex == null) {
            validUpgrades = ImmutableSortedSet.of();
            invalidUpgrades = ImmutableSortedSet.of();
            invisibleUpgrades = ImmutableSortedSet.of();
            activeUpgrade = null;
        } else {
            divideUpgrades(hex);
            if (validUpgrades.isEmpty()) {
                activeUpgrade = null;
            } else {
                activeUpgrade = validUpgrades.first();
            }
        }
    }
    
    private void changeActiveUpgrade(HexUpgrade upgrade) {
        if (upgrade == null) {
            if (activeUpgrade != null) {
                activeUpgrade.getHex().setUpgrade(null);
                activeUpgrade.getHex().update();
                activeUpgrade = null;
            }
        } else { // upgrade != null
            if (upgrade != activeUpgrade) {
                upgrade.firstSelection();
                upgrade.getHex().setUpgrade(upgrade);
                activeUpgrade = upgrade;
            } else {
                upgrade.nextSelection();
            }
            activeUpgrade.getHex().update();
        }

    }
    
    
    private void divideUpgrades(GUIHex hex) {
    
    
    }
    

    public GUIHex getActiveHex() {
        return activeHex;
    }

    public HexUpgrade getActiveUpgrade() {
        return activeUpgrade;
    }
    
    public HexUpgrade getNextUpgrade() {
        if (activeUpgrade == null) return null;
        
        if (validUpgrades.higher(activeUpgrade)) {
            
        }
    }
    
    /**
     * @return null if there is no or multiple Elements, otherwise the single one
     */
    public HexUpgrade singleValidElement(GUIHex hex) {
        HexUpgrade single = null;
        for (HexUpgrade upgrade:allUpgrades.get(hex)) {
            if (upgrade.isValid()) {
                if (single == null) { // first element => set single to this
                    single = upgrade;
                } else { // second element => return null
                    return null;
                }
            }
        }
        return single;
    }
    
    public void clear() {
        allBuilder = ImmutableSetMultimap.builder();
        allUpgrades = ImmutableSetMultimap.of();
        validUpgrades = ImmutableSortedSet.of();
        activeHex = null;
        activeUpgrade = null;
    }
    
}
