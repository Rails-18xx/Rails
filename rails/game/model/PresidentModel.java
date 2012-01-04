package rails.game.model;

import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.state.Item;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 */

public class PresidentModel extends Model<String> implements Observer {

    PublicCompany company;
    
    /**
     * PresidentModel is initialized with default id "PresidentModel"
     */
    public PresidentModel() {
        super("PresidentModel");
    }

    /**
     * Initialization of the PresidentModel
     * @param company
     */
    public void init(PublicCompany company) {
        super.init(company);
        this.company = company;
    }

    /** 
     * This method throws an IllegalArgumentException as PresidentModel works only for PublicCompanies
     */
    @Override
    public void init(Item parent){
        throw new IllegalArgumentException("PresidentModel init() only works for PublicCompanies");
    }

    public String getData() {
        Player president = company.getPresident();
        if (president == null ) return "";
        else return company.getPresident().getNameAndPriority();
    }

}
