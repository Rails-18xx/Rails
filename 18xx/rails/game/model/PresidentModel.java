package rails.game.model;

import java.util.Observable;
import java.util.Observer;

import rails.game.Player;
import rails.game.PublicCompanyI;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 */

public class PresidentModel extends ModelObject implements Observer {

    PublicCompanyI company;
    
    public PresidentModel(PublicCompanyI company) {
        this.company = company;
    }
       
    public void update(Observable o, Object arg) {
        // if notified by ShareModels, calls update itself
        update();
    }

    @Override
    public String getText() {
        Player president = company.getPresident();
        if (president == null ) return "";
        else return company.getPresident().getNameAndPriority();
    }

}
