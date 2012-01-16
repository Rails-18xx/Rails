package rails.game.model;

import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.state.Item;
import rails.game.state.Observer;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 */

public class PresidentModel extends Model implements Observer {

    PublicCompany company;
    
    /**
     * PresidentModel is initialized with default id "PresidentModel"
     */
    public PresidentModel() {
        super("PresidentModel");
    }

    /** 
     * Creates an initialized PresidentModel
     */
    public PresidentModel create(PublicCompany parent){
        return new PresidentModel().init(parent);
    }
    
    /** 
     * @param parent restricted to PublicCompany
     */
    @Override
    public PresidentModel init(Item parent){
        super.init(parent);
        if (parent instanceof PublicCompany) {
            this.company = (PublicCompany)parent;
        } else {
            throw new IllegalArgumentException("PresidentModel init() only works for PublicCompanies");
        }
        return this;
    }
    
    /**
     * @return restricted to PublicCompany
     */
    @Override
    public PublicCompany getParent() {
        return (PublicCompany)super.getParent();
    }

    @Override
    public String getText() {
        Player president = company.getPresident();
        if (president == null) return "";
        else return company.getPresident().getNameAndPriority();
    }

}
