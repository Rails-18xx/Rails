package rails.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.state.Item;
import rails.game.state.OwnableItem;

/**
 * FIXME: Use other mechanism (TokenManager) to store token ids
 * @author Erik Vos, Stefan Frey
 */
public abstract class Token extends OwnableItem<Token>  {

    protected String description = "";
    protected String uniqueId;
    
    // TODO: storing id in String is for legacy reasons
    protected static String STORAGE_NAME = "Token";

    protected static Logger log =
        LoggerFactory.getLogger(Token.class.getPackage().getName());
    
    // no public noarg constructor
    protected Token() {}
    
    /**
     * @throws IllegalArgumentException always, use init(Item parent) instead
     */
    @Override
    public void init(Item parent, String id) {
        throw new IllegalArgumentException("Token cannot be intialized with id, use init(Item parent) instead");
    }
    
    /** 
     * Token initialize without id
     * is generated automatically 
     * @param parent of the token
     */
    public abstract void init(Item parent); 
    
    @Override
    protected void checkedInit(Item parent, String id, Class<? extends Item> clazz) {
        uniqueId = STORAGE_NAME + "_" + GameManager.getInstance().storeObject(STORAGE_NAME, this);
        super.checkedInit(parent, id, clazz);
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
    
    // OwnableItem methods
//    public Portfolio<Token> getPortfolio() {
//        return portfolio;
//    }
//    
//    public void setPortfolio(Portfolio<Token> p) {
//        portfolio = p;
//    }
}
