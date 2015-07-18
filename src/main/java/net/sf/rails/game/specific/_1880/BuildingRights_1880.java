package net.sf.rails.game.specific._1880;

import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import net.sf.rails.game.Phase;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.model.RailsModel;
import net.sf.rails.game.state.HashSetState;

public class BuildingRights_1880 extends RailsModel {
    
    private static final BiMap<Integer, String> RIGHTS_TEXT_MAP = 
            ImmutableBiMap.of(0, "A", 1, "B", 2, "C", 3, "D");
    
    private static final Map<Integer, Integer> NB_RIGHTS_TO_PRESIDENT_SHARE = 
            ImmutableMap.of(2, 3, 3, 2, 4, 1);
    
    private final HashSetState<String> buildingRights = 
            HashSetState.create(this, "buildingRights"); 
    
    public BuildingRights_1880(RailsItem parent, String id) {
        super(parent, id);
    }    
    
    public void set(String rights) {
        // clear old rights first
        buildingRights.clear();
        // ... then add new rights
        for (String right: Splitter.on("+").split(rights))
            if (RIGHTS_TEXT_MAP.containsValue(right)) {
                buildingRights.add(right);
            }
    }
    
    public boolean canBuildInPhase(Phase phase) {
        // the phase (e.g. "A2") contains the required building right as the first character 
        String phase_1st = phase.getRealName().substring(0, 1);
        return buildingRights.contains(phase_1st);
    }

    @Override
    public String toText() {
        return Joiner.on("+").join(Ordering.natural().immutableSortedCopy(buildingRights));
    }
    
    public static List<String> getRightsForPresidentShareSize(int shares) {
        
        int nb_rights = NB_RIGHTS_TO_PRESIDENT_SHARE.get(shares);
        
        ImmutableList.Builder<String> options = ImmutableList.builder();
        int i = 0;
        while (i + nb_rights <= RIGHTS_TEXT_MAP.size()) {
            List<String> option = Lists.newArrayList();
            for (int k=0; k < nb_rights; k++) {
                option.add(RIGHTS_TEXT_MAP.get(k+i));
            }
            options.add(Joiner.on("+").join(option));
            i++;
        }
        return options.build();
    }
}
