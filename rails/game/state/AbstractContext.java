package rails.game.state;

import java.util.HashMap;
import java.util.Map;


/**
 * provides a skeleton implementation of a context
 * @author freystef
 */
public abstract class AbstractContext implements Context {
        final String id;
        final Map<String, Item> contextItems = new HashMap<String, Item>();
        
        public AbstractContext(String id) {
            this.id = id;
        }
        
        public Item localize(String id) {
            return contextItems.get(id);
        }

        public void addItem(Item item) {
            // checks if no key duplication
            assert(!contextItems.containsKey(item.getId()));
            
            contextItems.put(item.getId(), item);
        }
        
       public String getId() {
           return id;
       }
    
}
