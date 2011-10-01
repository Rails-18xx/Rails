package rails.game.model;

import rails.game.Player;
import rails.game.PublicCompany;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 */

public class PresidentModel extends AbstractModel<String> implements Observer {

    PublicCompany company;
    
    public PresidentModel(PublicCompany company) {
        super(company, "PresidentModel");
        this.company = company;
    }

    public String getData() {
        Player president = company.getPresident();
        if (president == null ) return "";
        else return company.getPresident().getNameAndPriority();
    }

}
