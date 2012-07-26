package rails.game.model;

import rails.game.BaseToken;
import rails.game.PublicCompany;
import rails.game.Token;
import rails.game.state.Model;
import rails.game.state.PortfolioSet;

/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends Model {

    private final PortfolioSet<Token> freeBaseTokens;
    private final PortfolioSet<Token> laidBaseTokens;

    private BaseTokensModel(PublicCompany parent, String id) {
        super(parent, id);
        freeBaseTokens = PortfolioSet.create(parent, "freeBaseTokens", Token.class);
        laidBaseTokens = PortfolioSet.create(parent, "laidBaseTokens", Token.class);
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
     * add a free base token
     */
    public void addFreeToken(BaseToken token) {
        freeBaseTokens.moveInto(token);
    }
    
    /**
     * lay a base token
     */
    public void layBaseToken(BaseToken token) {
        laidBaseTokens.moveInto(token);
    }
    
    public PortfolioSet<Token> getFreeTokens() {
        return freeBaseTokens;
    }
    
    public PortfolioSet<Token> getLaidTokens() {
        return laidBaseTokens;
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
        return laidBaseTokens.containsItem(token);
    }
    
    @Override 
    public String toString() {
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
