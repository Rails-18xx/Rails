package rails.game;

import org.apache.log4j.Logger;

import rails.game.model.AbstractOwnable;
import rails.game.state.GameItem;
import rails.game.state.OwnableItem;
import rails.game.state.Portfolio;

/**
 * @author Erik Vos
 */
public abstract class Token extends GameItem implements OwnableItem<Token>  {

    protected String description = "";
    protected String uniqueId;
    
    private Portfolio<Token> portfolio;
    
    // TODO: storing id in String is for legacy reasons
    protected static String STORAGE_NAME = "Token";

    protected static Logger log =
        Logger.getLogger(Token.class.getPackage().getName());

    public Token() {
        uniqueId = STORAGE_NAME + "_" + GameManager.getInstance().storeObject(STORAGE_NAME, this);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean equals(Token otherToken) {
        return otherToken != null && uniqueId.equals(otherToken.getUniqueId());
    }

    public static Token getByUniqueId(String id) {
        int i = Integer.valueOf(id.replace(STORAGE_NAME + "_", ""));
        return (Token)GameManager.getInstance().retrieveObject(STORAGE_NAME, i);
    }
    
    public Portfolio<Token> getPortfolio() {
        return portfolio;
    }
    
    public void setPortfolio(Portfolio<Token> p) {
        portfolio = p;
    }
}
