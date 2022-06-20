package net.sf.rails.game.model;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.rails.game.BaseToken;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.Portfolio;
import net.sf.rails.game.state.PortfolioSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import net.sf.rails.game.state.TreeSetState;


/**
 * A model presenting the number of tokens
 */
public class BaseTokensModel extends RailsModel {

    // the free tokens belong to the company
    private final PortfolioSet<BaseToken> freeBaseTokens;

    // a list of all base tokens, configured later
    // EV 6/2022: No longer Immutable, as in 1826 the quantity
    // of base tokens of most companies may increase during the game
    private TreeSetState<BaseToken> allBaseTokens;

    private BaseTokensModel(PublicCompany parent, String id) {
        super(parent, id);
        freeBaseTokens = PortfolioSet.create(parent, "freeBaseTokens", BaseToken.class);
        freeBaseTokens.addModel(this);
    }

    public static BaseTokensModel create(PublicCompany parent, String id){
        return new BaseTokensModel(parent, id);
    }

    /**
     * Initialize a set of tokens
     */
    public void initBaseTokens(SortedSet<BaseToken> tokens) {
        allBaseTokens = TreeSetState.create (this, "allBaseTokens", tokens);
        Portfolio.moveAll(allBaseTokens, getParent());
    }

    /**
     * Add more tokens than the initially configured number.
     * This is required for 1826.
     */
    public void addBaseToken(BaseToken token, boolean laid) {
        allBaseTokens.add(token);
        Portfolio.moveAll(allBaseTokens, getParent());
        if (!laid) freeBaseTokens.add(token);
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
    
    public SortedSet<BaseToken> getAllBaseTokens() {
        return allBaseTokens.getSet();
    }
    
    public ImmutableSet<BaseToken> getFreeTokens() {
        return freeBaseTokens.items();
    }
    
    public ImmutableSet<BaseToken> getLaidTokens() {
        return Sets.difference(allBaseTokens.view(), freeBaseTokens.items()).immutableCopy();
    }
    
    public int nbAllTokens() {
        return allBaseTokens.size();
    }
    
    public int nbFreeTokens() {
        return freeBaseTokens.size();
    }
    
    public int nbLaidTokens() {
        return allBaseTokens.size() - freeBaseTokens.size();
    }
    
    /**
     * @return true if token is laid
     */
    public boolean tokenIsLaid(BaseToken token) {
        return allBaseTokens.contains(token);
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
