package net.sf.rails.game.round;

import java.util.List;

import rails.game.action.PossibleAction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class AvailableActions {
    
    private List<PossibleAction> actions = Lists.newArrayList();
    
    public void addActions(List<PossibleAction> actions) {
        this.actions.addAll(actions);
    }
    
    public List<PossibleAction> getActions() {
        return ImmutableList.copyOf(actions);
    }

}
