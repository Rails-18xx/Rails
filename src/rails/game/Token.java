package rails.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.state.Item;
import rails.game.state.OwnableItem;

/**
 * FIXME: Use other mechanism (TokenManager) to store token ids
 * FIXME: UniqueId and id are a double structure
 */
public abstract class Token extends OwnableItem<Token>  {

    protected String description = "";
    protected String uniqueId;
    
    // TODO: storing id in String is for legacy reasons
    protected static String STORAGE_NAME = "Token";

    protected static Logger log =
        LoggerFactory.getLogger(Token.class.getPackage().getName());
    
    protected Token(Item parent, String id) {
        super(parent, id);
        uniqueId = id;
        GameManager.getInstance().storeObject(STORAGE_NAME, this);
    }
    
    /** 
     * @return Token unique_id 
     */
    protected static String createUniqueId() {
        return STORAGE_NAME + "_" + GameManager.getInstance().getStorageId(STORAGE_NAME);
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
}
