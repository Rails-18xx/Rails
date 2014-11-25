package net.sf.rails.ui.swing.elements;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import net.sf.rails.sound.SoundEventInterpreter;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
    
    private NavigableSet<HexUpgrade> hexUpgrades;
      
    private Map<HexUpgrade, UpgradeLabel> upgradeToLabels;

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
    
    public GUIHex getActiveHex() {
        return activeHex;
    }

    public HexUpgrade getActiveUpgrade() {
        return activeUpgrade;
    }
    
    public UpgradeLabel getActiveLabel() {
        if (activeUpgrade == null) {
            return null;
        } else {
            return upgradeToLabels.get(activeUpgrade);
        }
    }
    
    public ImmutableList<UpgradeLabel> getUpgradeLabels() {
        // copyOf used to change the return type, not the semantics
        return ImmutableList.copyOf(upgradeToLabels.values());
    }

    public void setActiveHex(GUIHex hex, int zoomStep) {
        
        // do nothing if hex is not different
        if (activeHex == hex) return;
        
        // delete activeUpgrade for previous activeHex
        if (activeHex != null) {
            changeActiveUpgrade(null);
        }

        activeHex = hex;
        if (hex == null) {
            hexUpgrades = ImmutableSortedSet.of();
        } else {
            hexUpgrades = ImmutableSortedSet.copyOf(allUpgrades.get(hex));
            defineLabels(zoomStep);
            HexUpgrade nextUpgrade = getNextUpgrade(null);
            changeActiveUpgrade(nextUpgrade);
        }
    }
    
    public void nextSelection() {
        if (activeUpgrade.hasSingleSelection()) {
            // do nothing
        } else {
            activeUpgrade.nextSelection();
            upgradeToLabels.get(activeUpgrade).updateIcon();
            activeHex.update();
            SoundManager.notifyOfSelectUpgrade(activeUpgrade);
        }
    }
    
    public void nextUpgrade() {
        HexUpgrade nextUpgrade = getNextUpgrade(activeUpgrade);
        changeActiveUpgrade(nextUpgrade);
    }
    
    public void setUpgrade(HexUpgrade upgrade) {
        if (upgrade.isValid()) {
            changeActiveUpgrade(upgrade);
        }
    }
    
    private void defineLabels(int zoomStep) {
        ImmutableMap.Builder<HexUpgrade, UpgradeLabel> labelsBuilder = ImmutableMap.builder();
        for (HexUpgrade upgrade:hexUpgrades) {
            if (upgrade.isVisible()) {
                UpgradeLabel label = new UpgradeLabel(upgrade, zoomStep);
                labelsBuilder.put(upgrade, label);
            }
        }
        upgradeToLabels = labelsBuilder.build();
    }

    private HexUpgrade getNextUpgrade(HexUpgrade after) {
        HexUpgrade candidate = after;            
        while (true) {
            if (candidate == null) {
                candidate = hexUpgrades.first();
            } else {
                candidate = hexUpgrades.higher(candidate);
            }
            if (candidate == after) { // all elements elapsed
                return after;
            }
            if (candidate == null) { // end of set
                continue;
            }
            if (candidate.isValid() && candidate.isVisible()) {
                return candidate;
            }
        }
    }

    private void changeActiveUpgrade(HexUpgrade upgrade) {
        if (upgrade == null) {
            if (activeUpgrade != null) {
                upgradeToLabels.get(activeUpgrade).setSelected(false);
                activeUpgrade = null;
                activeHex.setUpgrade(null);
                activeHex.update();
            }
            // upgrade == null, activeUpgrade == null => nothing to do
        } else { // upgrade != null
            if (upgrade != activeUpgrade) {
                if (activeUpgrade != null) {
                    upgradeToLabels.get(activeUpgrade).setSelected(false);
                }
                upgradeToLabels.get(upgrade).updateIcon();
                upgradeToLabels.get(upgrade).setSelected(true);
                activeUpgrade = upgrade;
                activeHex.setUpgrade(upgrade);
                activeHex.update();
            } else {
                // activate identical upgrade ==> next selection of that upgrade
                upgrade.nextSelection();
                upgradeToLabels.get(upgrade).updateIcon();
                activeHex.update();
            }
        }
        SoundManager.notifyOfSelectUpgrade(upgrade);
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
        hexUpgrades = ImmutableSortedSet.of();
        activeHex = null;
        activeUpgrade = null;
    }
    
}
