/**
 * 
 */
package net.sf.rails.game.specific._1844;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.model.RailsModel;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.HashSetState;

/**
 * @author martin
 *
 */
public class RightsModel_1844 extends RailsModel {

    
    private HashSetState<SpecialRight> rights = HashSetState.create(this, "rightsModel");

    /**
     * @param parent
     * @param id
     */
    public RightsModel_1844(HoldingCompany parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }
    
    public static RightsModel_1844 create(HoldingCompany parent, String id) {
        return new RightsModel_1844(parent, id);
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
