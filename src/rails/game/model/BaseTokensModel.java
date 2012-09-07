package rails.game.model;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import rails.game.BaseToken;
import rails.game.PublicCompany;
import rails.game.state.Model;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioSet;

/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends Model {

    // the free tokens belong to the company
    private final PortfolioSet<BaseToken> freeBaseTokens;
    // a list of all base tokens, configured later
    private ImmutableSortedSet<BaseToken> allTokens;

    private BaseTokensModel(PublicCompany parent, String id) {
        super(parent, id);
        freeBaseTokens = PortfolioSet.create(parent, "freeBaseTokens", BaseToken.class);
    }

    public static BaseTokensModel create(PublicCompany parent, String id){
        return new BaseTokensModel(parent, id);
    }

    /**
     * Initialize a set of tokens
     */
    public void initTokens(Set<BaseToken> tokens) {
        allTokens = ImmutableSortedSet.copyOf(tokens);
        Portfolio.moveAll(allTokens, getParent());
    }
    
    /**
     * @return parent the public company
     */
    @Override
    public PublicCompany getParent() {
        return (PublicCompany)super.getParent();
    }
    
    /**
     * @return the next (free) token to lay, null if none is available
     */
    public BaseToken getNextToken() {
        if (freeBaseTokens.size() == 0) return null;
        return Iterables.get(freeBaseTokens, 0);
    }
    
    public ImmutableSet<BaseToken> getAllTokens() {
        return allTokens;
    }
    
    public ImmutableSet<BaseToken> getFreeTokens() {
        return freeBaseTokens.items();
    }
    
    public ImmutableSet<BaseToken> getLaidTokens() {
        return Sets.difference(allTokens, freeBaseTokens.items()).immutableCopy();
    }
    
    public int nbAllTokens() {
        return allTokens.size();
    }
    
    public int nbFreeTokens() {
        return freeBaseTokens.size();
    }
    
    public int nbLaidTokens() {
        return allTokens.size() - freeBaseTokens.size();
    }
    
    /**
     * @return true if token is laid
     */
    public boolean tokenIsLaid(BaseToken token) {
        return allTokens.contains(token);
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
