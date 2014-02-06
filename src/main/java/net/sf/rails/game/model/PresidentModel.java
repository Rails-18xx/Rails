package net.sf.rails.game.model;

import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 * 
 * FIXME: Finalize implementation, this does not work currently
 * TODO: Check if this is all done correctly, where is the observable stored?
 */

public final class PresidentModel extends RailsModel {

    public static final String ID = "PresidentModel";  
    
    private PresidentModel(PublicCompany parent, String id) {
        super(parent, id);
    }

    public static PresidentModel create(PublicCompany parent){
        return new PresidentModel(parent, ID);
    }
    
    /**
     * @return restricted to PublicCompany
     */
    @Override
    public PublicCompany getParent() {
        return (PublicCompany)super.getParent();
    }

    @Override
    public String toText() {
        Player president = getParent().getPresident();
        if (president == null) return "";
        else return getParent().getPresident().getNameAndPriority();
    }

}
