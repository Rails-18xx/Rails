package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.game.PhaseI;

/**
 * @author Michael Alexander
 * 
 */

public class BuildingRights_1880 {
    
    private static final Map<Integer, String> RIGHTS_TEXT_MAP = createMap();

    private static Map<Integer, String> createMap() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        result.put(0, "A");
        result.put(1, "B");
        result.put(2, "C");
        result.put(3, "D");
        return Collections.unmodifiableMap(result);
    }
    
    private final static int NUM_RIGHTS = 4;
    
    private BitSet buildingRights = new BitSet(NUM_RIGHTS); 

    public BuildingRights_1880() {
        for (int i = 0; i < NUM_RIGHTS; i++) {
            buildingRights.clear(i);            
        }
    }
    
    public String rightsString() {
        StringBuffer text = new StringBuffer();
        boolean addPlus = false;
        for (int i = 0; i < NUM_RIGHTS; i++) {
            if (buildingRights.get(i) == true) {
                if (addPlus == true) {
                    text.append("+");
                } else {
                    addPlus = true;
                }
                text.append(RIGHTS_TEXT_MAP.get(i));
            }
        }
        return text.toString();
    }

    public boolean canBuildInPhase(PhaseI phase) {
        boolean canBuild = false;
        for (int i = 0; i < NUM_RIGHTS; i++) {
            if (phase.getRealName().startsWith(RIGHTS_TEXT_MAP.get(i))) {
                canBuild = buildingRights.get(i);
                break;
            }
        }
        return canBuild;
    }

    public void setRights(String buildingRightsString) {
        for (int i = 0; i < NUM_RIGHTS; i++) {
            if (buildingRightsString.contains(RIGHTS_TEXT_MAP.get(i))) {
                buildingRights.set(i);
            } else {
                buildingRights.clear(i);
            }
        }
    }

    public static String[] getRightsForPresidentShareSize(int shares) {
        List<String> options = new ArrayList<String>();        
        for (int i = 0; i < NUM_RIGHTS; i++) {
            options.add(RIGHTS_TEXT_MAP.get(i));
        }
        if (shares <= 3) {
            for (int i = 0; i < (NUM_RIGHTS - 1); i++) {
                options.add(RIGHTS_TEXT_MAP.get(i) + "+" + RIGHTS_TEXT_MAP.get(i+1));
            }            
        }
        if (shares <= 2) {
            for (int i = 0; i < (NUM_RIGHTS - 2); i++) {
                options.add(RIGHTS_TEXT_MAP.get(i) + "+" + RIGHTS_TEXT_MAP.get(i+1) + "+" + RIGHTS_TEXT_MAP.get(i+2));
            }                        
        }
        
        return options.toArray(new String[options.size()]);
    }

}
