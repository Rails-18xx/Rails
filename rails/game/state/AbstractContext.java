package rails.game.state;

import java.util.HashMap;
import java.util.Map;

/**
 * provides a skeleton implementation of a context
 * @author freystef
 */
public abstract class AbstractContext extends AbstractItem implements Context {
        final Map<String, Item> items = new HashMap<String, Item> ();
        
        /**
         * Creates a AbstractContext
         * @param id identifier (cannnot be null)
        
         * Remark: A top-level context does not need to be initialized
         */
        public AbstractContext(String id) {
            super(id); // AbstractItem checks for not-null already
        }

        // Overwrite for context method to return itself 
        @Override
        public Context getContext() {
            return this;
        }
        
        // Context interface
        public Item localize(String uri) {
            if (items.containsKey(uri)) {
                return items.get(uri);
            } else if (getParent() != null) {
                return getContext().localize(uri);
            } else { 
                return null;
            }
        }

        public void addItem(Item item) {
            // first check if this context is the containing one
            String uri;
            if (item.getContext() == this) {
                uri = item.getURI();
            } else {
                uri = item.getContext().getURI() + Context.SEP + item.getURI();
            }
            
            // check if it exists
            if (items.containsKey(uri)) {
                throw new RuntimeException("Context already contains item with identical URI = " + item.getURI());
            }
            
            // otherwise put it to the items list
            items.put(uri, item);
            
            // forward to parent context if that is defined
            if (getContext() != null) {
                getContext().addItem(item);
            }
            
        }
        
    
}
