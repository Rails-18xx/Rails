package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.game.PhaseI;
import rails.game.state.StringState;

/**
 * @author Michael Alexander
 * 
 */

public class BuildingRights_1880 extends StringState {
    
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
    
    public BuildingRights_1880(String name) {
        super(name, "");
        for (int i = 0; i < NUM_RIGHTS; i++) {
            buildingRights.clear(i);
        }
    }    
    
    public String getText() {
        return super.stringValue();
    }

    public void set(String string) {
        super.set(string);
        for (int i = 0; i < NUM_RIGHTS; i++) {
            if (string.contains(RIGHTS_TEXT_MAP.get(i))) {
                buildingRights.set(i);
            } else {
                buildingRights.clear(i);
            }
        }
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
