package rails.game.model;

import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.state.Item;
import rails.game.state.Observable;
import rails.game.state.Observer;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 * 
 * TODO: Check if this is all done correctly, where is the observable stored?
 */

public class PresidentModel extends Model implements Observer {

    public static final String ID = "PresidentModel";  
    
    PublicCompany company;
    
    private PresidentModel() {
        super(ID);
    }

    /** 
     * Creates an owned PresidentModel with ID "PresidentModel"
     */
    public static PresidentModel create(PublicCompany parent){
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
