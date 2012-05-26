package rails.game.model;

import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.game.state.Observable;
import rails.game.state.Observer;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 * 
 * TODO: Check if this is all done correctly, where is the observable stored?
 */

public class PresidentModel extends Model implements Observer {

    public static final String id = "PresidentModel";  
    
    PublicCompany company;
    
    private PresidentModel() {}

    public static PresidentModel create(){
        return new PresidentModel();
    }
    
    /** 
     * @param parent restricted to PublicCompany
     */
    @Override
    public void init(Item parent, String id){
        super.checkedInit(parent, id, PublicCompany.class);
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

    public Observable getObservable() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean deRegister() {
        // TODO Auto-generated method stub
        return false;
    }

}
