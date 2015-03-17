package net.sf.rails.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: Use other mechanism (TokenManager) to store token ids
 * FIXME: UniqueId and id are a double structure
 */
public abstract class Token<T extends Token<T>> extends RailsOwnableItem<T> implements Upgrade {

    protected String description = "";
    protected String uniqueId;
    
    // TODO: storing id in String is for legacy reasons
    protected static String STORAGE_NAME = "Token";

    protected static Logger log =
        LoggerFactory.getLogger(Token.class);
    
    protected Token(RailsItem parent, String id, Class<T> clazz) {
        super(parent, id, clazz);
        uniqueId = id;
        parent.getRoot().getGameManager().storeObject(STORAGE_NAME, this);
    }
    
    @Override
    public RailsItem getParent() {
        return (RailsItem)super.getParent();
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
    }
    
    public String getUniqueId() {
        return uniqueId;
    }

    // TODO: Rails 2.0 Move it to Token manager 
    
    /** 
     * @return Token unique_id 
     */
    protected static String createUniqueId(RailsItem item) {
        return STORAGE_NAME + "_" + item.getRoot().getGameManager().getStorageId(STORAGE_NAME);
    }

    public static <T extends Token<T>> T getByUniqueId(RailsItem item, Class<T> clazz, String id) {
        int i = Integer.valueOf(id.replace(STORAGE_NAME + "_", ""));
        return clazz.cast(item.getRoot().getGameManager().retrieveObject(STORAGE_NAME, i));
    }
}
