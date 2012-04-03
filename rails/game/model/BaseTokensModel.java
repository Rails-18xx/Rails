package rails.game.model;

import static com.google.common.base.Preconditions.checkArgument;

import rails.game.BaseToken;
import rails.game.PublicCompany;
import rails.game.Token;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.game.state.PortfolioList;

/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends Model {

    private PortfolioList<Token> freeBaseTokens = PortfolioList.create();
    private PortfolioList<Token> laidBaseTokens = PortfolioList.create();

    private BaseTokensModel() {}

    public static BaseTokensModel create(){
        return new BaseTokensModel();
    }

    /** 
     * @param parent restricted to PublicCompany
     */
    @Override
    public BaseTokensModel init(Item parent, String id){
        // init parent
        checkArgument(parent instanceof PublicCompany, "BaseTokenModel init() only works for PublicCompanies");
        super.init(parent, id);
        
        // Init states
        freeBaseTokens.init(parent, "freeBaseTokens");
        laidBaseTokens.init(parent, "laidBaseTokens");
        
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
    
    public PortfolioList<Token> getFreeTokens() {
        return freeBaseTokens;
    }
    
    public PortfolioList<Token> getLaidTokens() {
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
