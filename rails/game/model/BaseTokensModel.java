package rails.game.model;

import rails.game.PublicCompany;
import rails.game.state.Item;
import rails.game.state.State;

/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends Model {

    public static final String ID = "BaseTokensModel";

    private PublicCompany company;

    /**
     * BaseTokenModel is initialized with a default id "BaseTokensModel"
     */
    private BaseTokensModel() {
        super(ID);
    }

    /**
     * Creates an owned BaseTokenModel
     */
    public static BaseTokensModel create(PublicCompany company){
        return new BaseTokensModel().init(company);
    }
    
    /**
     * Creates an unowned BaseTokenModel
     */
    public static BaseTokensModel create(){
        return new BaseTokensModel();
    }

    /** 
     * @param parent restricted to PublicCompany
     */
    @Override
    public BaseTokensModel init(Item parent){
        super.init(parent);
        if (parent instanceof PublicCompany) {
            this.company = (PublicCompany)parent;
        } else {
            throw new IllegalArgumentException("BaseTokenModel init() only works for PublicCompanies");
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
    
    /**
     * Sets the states related to the model
     * @param allTokenState
     * @param freeTokenState
     * TODO: Replace with a model that contains the states
     */
    public BaseTokensModel setStates(State allTokenState, State freeTokenState){
        allTokenState.addModel(this);
        freeTokenState.addModel(this);
        return this;
    }

    @Override 
    public String toString() {
        int allTokens = company.getNumberOfBaseTokens();
        int freeTokens = company.getNumberOfFreeBaseTokens();
        if (allTokens == 0) {
            return "";
        } else if (freeTokens == 0) {
            return "-/" + allTokens;
        } else {
            return freeTokens + "/" + allTokens;
        }
    }

}
