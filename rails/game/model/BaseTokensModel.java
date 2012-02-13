package rails.game.model;

import static com.google.common.base.Preconditions.checkArgument;
import rails.game.PublicCompany;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.game.state.State;

/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends Model {

    private PublicCompany company;

    private BaseTokensModel() {}

    public static BaseTokensModel create(){
        return new BaseTokensModel();
    }

    /** 
     * @param parent restricted to PublicCompany
     */
    @Override
    public BaseTokensModel init(Item parent, String id){
        checkArgument(parent instanceof PublicCompany, "BaseTokenModel init() only works for PublicCompanies");
        super.init(parent, id);
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
