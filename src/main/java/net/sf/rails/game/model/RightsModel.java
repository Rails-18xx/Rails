package net.sf.rails.game.model;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.HashSetState;

/**
 * RightsModel stores purchased SpecialRight(s)
 *
 */
public class RightsModel extends RailsModel {

    private HashSetState<SpecialRight> rights = HashSetState.create(this, "rightsModel");
    
    private RightsModel(PublicCompany parent, String id) {
        super(parent, id);
    }
    
    public static RightsModel create(PublicCompany parent, String id) {
        return new RightsModel(parent, id);
    }
    
    @Override
    public PublicCompany getParent() {
        return (PublicCompany)getParent();
    }
    
    public void add(SpecialRight right) {
        rights.add(right);
    }
    
    public boolean contains(SpecialRight right) {
        return rights.contains(right);
    }
    
    public String toText() {
        ImmutableList.Builder<String> rightsText = ImmutableList.builder();
        for (SpecialRight right:rights) {
            rightsText.add(right.getName());
        }
        return Joiner.on(",").join(rightsText.build()).toString();
    }
    
}
