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
     * Initialization of a BaseTokenModels requires the definition of the following parameters
     * @param company 
     * @param allTokenState
     * @param freeTokenState
     */
    public void init(PublicCompany company, State allTokenState, State freeTokenState){
        super.init(company);
        this.company = company;
        allTokenState.addModel(this);
        freeTokenState.addModel(this);
    }
    
    /** 
     * This method throws an IllegalArgumentException as BaseTokenModels requires more attributes
     */
    @Override
    public void init(Item parent){
        throw new IllegalArgumentException("BaseTokenModel init() requires additional parameters");
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
