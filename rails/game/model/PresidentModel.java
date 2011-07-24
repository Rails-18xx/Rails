package rails.game.model;

import rails.game.Player;
import rails.game.PublicCompanyI;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 */

public class PresidentModel extends AbstractModel<String> implements View<String> {

    PublicCompanyI company;
    
    public PresidentModel(PublicCompanyI company) {
        super(company, "PresidentModel");
        this.company = company;
    }

    public String getData() {
        Player president = company.getPresident();
        if (president == null ) return "";
        else return company.getPresident().getNameAndPriority();
    }

    public void update(String data) {
        // pass along the update as a viewer
        notifyModel();
    }

}
