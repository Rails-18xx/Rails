package rails.game.model;

import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.state.Model;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 * 
 * FIXME: Finalize implementation, this does not work currently
 * TODO: Check if this is all done correctly, where is the observable stored?
 */

public final class PresidentModel extends Model {

    public static final String ID = "PresidentModel";  
    
    PublicCompany company;
    
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
    public String toString() {
        Player president = company.getPresident();
        if (president == null) return "";
        else return company.getPresident().getNameAndPriority();
    }

}
