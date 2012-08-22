package rails.game.model;

import com.google.common.collect.ImmutableSet;

import rails.game.BaseToken;
import rails.game.PublicCompany;
import rails.game.state.HashSetState;
import rails.game.state.Model;
import rails.game.state.PortfolioSet;

/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends Model {

    // the free tokens belong to the company
    private final PortfolioSet<BaseToken> freeBaseTokens;
    // the laid tokens are only be referenced
    private final HashSetState<BaseToken> laidBaseTokens;

    private BaseTokensModel(PublicCompany parent, String id) {
        super(parent, id);
        freeBaseTokens = PortfolioSet.create(parent, "freeBaseTokens", BaseToken.class);
        laidBaseTokens = HashSetState.create(parent, "laidBaseTokens");
    }

    public static BaseTokensModel create(PublicCompany parent, String id){
        return new BaseTokensModel(parent, id);
    }
    
    /**
     * @return restricted to PublicCompany
     */
    @Override
    public PublicCompany getParent() {
        return (PublicCompany)super.getParent();
    }
    
    /**
     * Adds a BaseToken (back) to the list of free token
     * Removes it from the LaidTokenList
     * Remark: It is automatically removed from the previous owner portfolio
     * @return true if it was previously on the LaidTokenList
     */
    public boolean addFreeToken(BaseToken token) {
        freeBaseTokens.moveInto(token);
        return laidBaseTokens.remove(token);
    }
    
    public ImmutableSet<BaseToken> getFreeTokens() {
        return freeBaseTokens.items();
    }
    
    public ImmutableSet<BaseToken> getLaidTokens() {
        return laidBaseTokens.view();
    }
    
    public int nbAllTokens() {
        return nbFreeTokens() + nbLaidTokens();
    }
    
    public int nbFreeTokens() {
        return freeBaseTokens.size();
    }
    
    public int nbLaidTokens() {
        return laidBaseTokens.size();
    }
    
    /**
     * @return true if token is laid
     */
    public boolean tokenIsLaid(BaseToken token) {
        return laidBaseTokens.contains(token);
    }
    
    @Override 
    public String toText() {
        int allTokens = nbAllTokens();
        int freeTokens = nbFreeTokens();
        if (allTokens == 0) {
            return "";
        } else if (freeTokens == 0) {
            return "-/" + allTokens;
        } else {
            return freeTokens + "/" + allTokens;
        }
    }

}
