package net.sf.rails.ui.swing.elements;

import java.util.Set;

import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Class that wraps a Multimap between GuiHex and MapUpgrades
 */
public class GuiHexUpgrades {

    private SetMultimap<GUIHex, HexUpgrade> map;
    
    private ImmutableSetMultimap.Builder<GUIHex, HexUpgrade> builder;
    
    private GuiHexUpgrades() {
        builder = ImmutableSetMultimap.builder();
        map = ImmutableSetMultimap.of();
    }
    
    public static GuiHexUpgrades create() {
        return new GuiHexUpgrades();
    }
    
    public void put(GUIHex hex, HexUpgrade upgrade) {
        builder.put(hex, upgrade);
    }

    public void putAll(GUIHex hex, Set<? extends HexUpgrade> upgrades) {
        for (HexUpgrade upgrade:upgrades) {
            builder.put(hex, upgrade);
        }
    }
    
    public void build() {
        map = builder.build();
        builder = ImmutableSetMultimap.builder();
    }
    
    public Set<HexUpgrade> getUpgrades(GUIHex hex) {
        return map.get(hex);
    }
    
    public boolean contains(GUIHex hex) {
        return map.containsKey(hex);
    }
    
    public Set<GUIHex> getHexes() {
        return map.keySet();
    }
    
    public boolean hasElements() {
        return !map.isEmpty();
    }
    
    /**
     * @return null if there is no or multiple Elements, otherwise the single one
     */
    public HexUpgrade singleValidElement(GUIHex hex) {
        HexUpgrade single = null;
        for (HexUpgrade upgrade:map.get(hex)) {
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
        builder = ImmutableSetMultimap.builder();
        map = ImmutableSetMultimap.of();
    }
    
}
