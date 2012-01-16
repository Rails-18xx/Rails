package rails.game.model;

import rails.game.PublicCompany;
import rails.game.state.Item;
import rails.game.state.State;

/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends Model {

    private PublicCompany company;

    /**
     * BaseTokenModel is initialized with a default id "BaseTokensModel"
     */
    public BaseTokensModel() {
        super("BaseTokensModel");
    }

    /**
     * Creates an initialized BaseTokenModel
     */
    public static BaseTokensModel create(PublicCompany company){
        return new BaseTokensModel().init(company);
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
    public void setStates(State allTokenState, State freeTokenState){
        super.init(company);
        allTokenState.addModel(this);
        freeTokenState.addModel(this);
    }

    @Override 
    protected String getText() {
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
